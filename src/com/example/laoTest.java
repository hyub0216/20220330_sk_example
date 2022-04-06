package com.example;

import yahoofinance.Stock;
import yahoofinance.YahooFinance;
import yahoofinance.histquotes.HistoricalQuote;
import yahoofinance.histquotes.Interval;

import java.io.IOException;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class laoTest {

         /*
        회차계산방법
         - 총매입액*40 / 시드 (소수점 두번째 자리에서 반올림)
         - 총매입액 = 평단가*보유량 (소수점 두번째 자리에서 반올림)

        매수기준점
         - LOC평단 = 내 평단 소수점 세번째 자리에서 반올림
         - LOC큰수
               - 20회차 미만이고 종가*1.12 > 평단*1.05 면 평단*1.05
               - 20회차 미만이고 종가*1.12 <= 평단*1.05면 종가*1.12
               - 20회차 이상이면 ""

        전반전 매도
         - LOC5%
               - 수량 : 보유량의 25%
               - 가격 : 평단가*1.052 세번째자리 반올림
         - 지정가10%
               - 수량 : 보유량의 75%
               - 가격 : 평단가*1.102 세번째자리 반올림

        후반전 매도
         - LOC0%
               - 수량 : 보유량의 25%
               - 가격 : 평단가*1.002 세번째자리 반올림
         - 지정가5%
               - 수량 : 보유량의 25%
               - 가격 : 평단가*1.052 세번째자리 반올림
         - 지정가10%
               - 수량 : 보유량의 50%
               - 가격 : 평단가*1.102 세번째자리 반올림
        */
    private static final String DRIVER ="com.mysql.cj.jdbc.Driver";
    private static final String URL ="jdbc:mysql://localhost:3306/sk_example?useSSL=false";
    private static final String USER = "root";
    private static final String PASS = "root";
    private static Connection conn = null;
    private static PreparedStatement ps = null;
    private static List<TargetStock> targetStocks = new ArrayList<>();
    private static List<Summary> list = new ArrayList();
    private static final int [] DIVISIONS ={
//            10,20,30,
            40
//            ,50,60,70,80,90,100
    };
    private static final String[] SYMBOLS ={"TQQQ"
//            ,"FNGU","SOXL","TECL","TNA","UPRO"
    };
    private static final String[] YEARS ={"2013"
//            ,"2012","2013","2014","2015","2016","2017","2018","2019","2020","2021","2022"
    };
    private static final double SEED_MONEY = 40000.0;

    static class Summary{

        String symbol;
        String year;
        double incomePer;
        int division;
        Map<String, Integer> failHistories;

        Summary(){
            symbol="";
            year ="";
            division =0;
            incomePer =0;
            failHistories = new HashMap<>();
        }
    }

    static class TargetStock{

        String symbol;
        String standardDate;
        double closePrice;
        double highPrice;

        TargetStock(String pSymbol, String pStandardDate, double pClosePrice, double pHightPrice ){
            symbol = pSymbol;
            standardDate = pStandardDate;
            closePrice = pClosePrice;
            highPrice = pHightPrice;
        }
    }

    static class Investor {

        double seedMoney; // 시드머니
        double totalBalance; // 잔액
        int totalMount; // 갖고있는 갯수
        double averagePrice;// 평가
        double totalBuyMoney; //구매 총 금액
        double sellMoney; //판매 총 금액
        double buyMoneyValue; // 평가금액
        boolean isFailed ;
        double bigBuyNumber; //LOC큰수 : 전날 종가와 평단가를 가지고 결정
        double lastDayClosePrice; //전날종가
        int division; //몇분할할것인지
        double incomePercent; //수익률
        double availableBuyMoney; //구매가능잔고

        Investor()
        {
        }

        void init(double pSeedMoney, int pDivision)
        {
            seedMoney =pSeedMoney;
            division= pDivision;
            totalMount =0;
            averagePrice = 0;
            totalBuyMoney =0;
            totalBalance =pSeedMoney;
            availableBuyMoney =pSeedMoney;
            buyMoneyValue =0;
            sellMoney =0;
            isFailed =false;
            bigBuyNumber =0;
            lastDayClosePrice =0;
            incomePercent=0;

        }
    }

    private static Investor investor = new Investor();
    private static double incomeMoneyTotal = 0;

    public static void main(String[] args) {

        /*
        * input목록
        * 종목심볼 : ex)TQQQ
        * 분석시작년월 ~ 분석종료년월
        * 몇분할로 할껀지
        * 시드머니
        * */

        int firstRoundLimit;

        for(String targetYear : YEARS)
        {
            for(String symbol : SYMBOLS){
                saveStock(symbol);
                for(int division : DIVISIONS)
                {
                    firstRoundLimit = division /2 ;//전후반 기준설정

                    investor.init(SEED_MONEY,division);
                    String failStandardDate;
                    Map<String, Integer> failHistory = new HashMap<>();
                    double roundNum =0;
                    double dayLimitMoney = SEED_MONEY/division;
                    int dayBuyMaxCount =0;
                    int locAverageCount = 0;
                    int locBigCount =0;

                    for(TargetStock targetStock : targetStocks)
                    {
                            String strdDt = targetStock.standardDate.substring(0,4);
                            if(targetYear.equals(strdDt))
                            {
                                System.out.println();

                                int totalCount = investor.totalMount;  //총보유수량

                                //이전 거래종료 후 평단가로 결정되는 LOC,지정가 들은 새로운 거래 전에설정
                                double loc5 = getRoundDouble(investor.averagePrice*1.052,3);
                                int loc5SellCount = getIntFromDouble(totalCount,0.25);

                                double fix5 = getRoundDouble(investor.averagePrice*1.052,3);
                                int fix5SellCount =  getIntFromDouble(totalCount,0.25); //전체수량의 25%

                                double loc0 = getRoundDouble(investor.averagePrice*1.002,3);
                                int loc0SellCount = getIntFromDouble(totalCount,0.25);

                                double fix10 = getRoundDouble(investor.averagePrice*1.102,3);
                                int fix10SellCount = investor.totalMount - fix5SellCount - loc0SellCount;
                                int firstRoundFix10SellCount = investor.totalMount -loc5SellCount;

                                double locAveragePrice = getRoundDouble(investor.averagePrice,3); // 평단가
                                double averagePrice = investor.averagePrice; // 평단가
                                //가격 형성및 수량 설정 종료

                                if(investor.totalMount ==0) // 처음 시작할때는 종가로 무조건 구매를 한다.
                                {
                                    investor.init(SEED_MONEY,division);
                                    int count = (int) (dayLimitMoney / targetStock.closePrice);//종가기준으로 구매
                                    buy(count, targetStock.closePrice, "전반전 시작 매수");
                                }
                                else if(roundNum < firstRoundLimit) //전반전일경우
                                {
                                    //LOC평단가 매수
                                    if(targetStock.closePrice <= locAveragePrice) // 종가가 내 평단가 이하인경우 매수
                                    {
                                        if(locAverageCount > 0)
                                        {
                                            buy(locAverageCount, targetStock.closePrice,"전반전 LOC평단매수");
                                        }
                                    }
                                    //LOC큰수 매수
                                    if(targetStock.closePrice <= investor.bigBuyNumber)
                                    {
                                        if(locBigCount > 0)
                                        {
                                            buy(locBigCount,targetStock.closePrice, "전반전 LOC큰수 매수");
                                        }
                                    }

                                    if(targetStock.closePrice >= loc5)//매도 25%
                                    {
                                        //종가 > loc5
                                        //25%매도하고끝
                                        if(loc5SellCount>0)
                                        {
                                            sell(loc5SellCount,targetStock.closePrice,"전판전 LOC5매도");
                                        }
                                    }

                                    if(targetStock.highPrice >= fix10) // 매수가 발생해도 발생할수있음
                                    {
                                        // 장중고가 >= loc10
                                        if(fix10SellCount>0)
                                        {
                                          sell(firstRoundFix10SellCount,fix10, "전반전 지정가10매도");
                                        }
                                    }
                                }
                                else if(roundNum >=firstRoundLimit && roundNum<division)//후반전
                                {
                                    if(targetStock.closePrice <= averagePrice) //LOC평단매수
                                    {
                                        // 종가 <= 내평단가
                                        if(dayBuyMaxCount > 0)
                                        {
                                            buy(dayBuyMaxCount,targetStock.closePrice,"후반전 LOC평단매수");
                                        }
                                    }

                                    //종가가 loc0보다 크거나 같으면 LOC0 매도
                                    if(targetStock.closePrice >= loc0)
                                    {
                                        // 내 평단가 < 종가 < loc5
                                        //25%매도하고끝
                                        if(loc0SellCount>0)
                                        {
                                            sell(loc0SellCount,targetStock.closePrice,"후반전 LOC0매도");
                                        }
                                    }

                                    //고가가 지정가 5 보다 크거나 같으면 지정가5 매도
                                    if(targetStock.highPrice >= fix5)
                                    {
                                        if(fix5SellCount>0)
                                        {
                                            sell(fix5SellCount,loc5,"후반전 지정가5매도");
                                        }
                                    }

                                    //고가가 지정가 지정가 10 보다 크거나 같으면 지정가10 매도
                                    //지정가 10매도가 되면 지정가 5도 매도된다고 생각할수있음
                                    if(targetStock.highPrice >= fix10)
                                    {
                                        if(fix10SellCount>0)
                                        {
                                            sell(fix10SellCount,fix10,"후반전 지정가10매도")   ;
                                        }
                                    }
                                }

                                //마지막회차 이상일때
                                if(roundNum >=division)
                                {
                                    if(totalCount > 0)//남은 갯수가 있다면
                                    {
                                        if(investor.averagePrice > targetStock.closePrice) // 종가가 평단가보다 낮다면 10회차치 손절
                                        {
                                            //개수 : 10라운드를 진행할수 있을 만큼의 수량을 매도
                                            int count  = (int)(dayLimitMoney*10 / targetStock.closePrice)+1;
                                            sell(count,targetStock.closePrice,division+"회차 손절");
                                            investor.isFailed =true;
                                        }
                                        else{ //아니면 남은 수량 익절
                                            sell(investor.totalMount,targetStock.closePrice,division+"회차 정산");
                                        }
                                    }
                                }

                                investor.buyMoneyValue = getRoundDouble(targetStock.closePrice* investor.totalMount,5);// 평가금액 : 보유량 * 종가
                                roundNum = getRoundNum();//거래가 끝난다음에 해당회차의 라운드를 가져올수있음
                                System.out.println(targetStock.standardDate+ "-"+roundNum+"회차-"
                                                +"   종가 :  "+targetStock.closePrice
                                                +" , 평단가 : "+investor.averagePrice
                                                +" , 보유수량 : "+investor.totalMount
                                                +" , 구매가능잔고 : "+investor.availableBuyMoney
                                                +" , 구매가능 수량 : "+dayBuyMaxCount
                                                +" , 총매입액 : "+investor.totalBuyMoney
                                                +" , 평가금액 : "+investor.buyMoneyValue
                                                +" , LOC큰수 : "+investor.bigBuyNumber
                                                +" , LOC평단가 : "+locAveragePrice
                                                +" , LOC0 : "+loc0
                                                +" , LOC5 : "+loc5
                                                +" , FIX5 : "+fix5
                                                +" , FIX10 : "+fix10
                                                +" , 고가 :  "+targetStock.highPrice);


                                if(investor.isFailed)//손절했다면 기록
                                {
                                    failStandardDate = targetStock.standardDate.substring(0,6);
                                    investor.isFailed =false;

                                    if(failHistory.get(failStandardDate)!=null)
                                    {
                                        int failCountTemp = failHistory.get(failStandardDate)+1;
                                        failHistory.replace(failStandardDate,failCountTemp);
                                    }
                                    else
                                    {
                                        failHistory.put(failStandardDate,1);
                                    }
                                }

                                //전날 종가로 변경되는 값들은 거래를 다 끝난후에 세팅한다.
                                investor.lastDayClosePrice = targetStock.closePrice;//전날 종가 저장하기

                                //하루 구매할수 있는 수량은 초기자금 / 분할횟수 / 이전거래일 장종가
                                dayBuyMaxCount = (int)Math.floor(dayLimitMoney / targetStock.closePrice);
                                locAverageCount =  dayBuyMaxCount/2;
                                locBigCount = dayBuyMaxCount - locAverageCount;

                                //LOC큰수는 전날 거래결과로 결정
                                if(targetStock.closePrice*1.12 > investor.averagePrice*1.05)// 종가의 1.12배가
                                {
                                    investor.bigBuyNumber = getRoundDouble(investor.averagePrice*1.05,3);
                                }
                                else
                                    investor.bigBuyNumber = getRoundDouble(targetStock.closePrice*1.12,3);

                                //전날 종가로 변경되는 값들은 거래를 다 끝난후에 세팅종료

                          }
                    }

                    // 모든 거래가 끝나고 수익률 계산 - 총가치 / 시작머니 *100
                    double incomePercent = (investor.buyMoneyValue+investor.availableBuyMoney) / SEED_MONEY*100;
                    Summary summary = new Summary();
                    summary.symbol=symbol;
                    summary.incomePer = getRoundDouble(incomePercent-100,3);
                    summary.year =targetYear;
                    summary.division = division;
                    for(String key : failHistory.keySet())
                    {
                        int value = failHistory.get(key);
                        summary.failHistories.put(key,value);
                    }
                    list.add(summary);
                    saveDB(summary);
                    failHistory.clear();
                }
                print();
        }
        }

    }

    private static void print() {

        for(Summary summary : list){
            System.out.println();
            System.out.println(summary.year+"년");
            System.out.println("분할횟수 : "+summary.division);
            System.out.println("수익률 : "+incomeMoneyTotal/SEED_MONEY*100);
            System.out.println("수익금 : "+incomeMoneyTotal);
            System.out.println("손절 ");
            for(String key  : summary.failHistories.keySet())
            {
                System.out.println(key+" : "+summary.failHistories.get(key));
            }
        }
    }

    private static int getIntFromDouble(double target, double percent)
    {
       return Long.valueOf(Math.round(target * percent)).intValue();
    }

    private static void sell(int count, double price, String coCl) {
        double daySellMoney = count*price; //판매금액
        investor.totalMount -=count; //총 보유수량 감소
        investor.totalBuyMoney = getRoundDouble(investor.totalBuyMoney -(investor.averagePrice*count),5); // 총 구매금액 감소
        investor.availableBuyMoney =getRoundDouble(investor.availableBuyMoney +daySellMoney,2); //구매가능금액 증가
        // 단순 판매라 평단가 영향 없음
        investor.totalBalance = getRoundDouble(investor.totalBalance +daySellMoney,5); // 남은 잔고 증가
        investor.sellMoney =getRoundDouble(investor.sellMoney+count* investor.averagePrice,5);
        double incomeMoney = getRoundDouble((price - investor.averagePrice) * count-(0.002*price*count),5);
        incomeMoneyTotal+=incomeMoney;
        System.out.println("#"+coCl+" sell count : "+count+ " , price : "+price+" , 수익금액 : "+incomeMoney);
    }

    private static void buy(int count, double price, String coCl) {
        System.out.println("#"+coCl+" buy count : "+count+ " , price : "+price);
        double dayBuyMoney = count*price; // 구매금액
        investor.availableBuyMoney =getRoundDouble(investor.availableBuyMoney -dayBuyMoney,2); //구매가능잔고 감소
        investor.totalMount += count; //총 보유량 증가
        investor.totalBuyMoney =getRoundDouble(investor.totalBuyMoney +dayBuyMoney,2); //총 구매금액 증가
        investor.averagePrice = getRoundDouble(investor.totalBuyMoney / investor.totalMount ,3); //평단가 변경
        investor.totalBalance = getRoundDouble(investor.totalBalance -dayBuyMoney,5); //남은 잔고변경
    }

    public static double getRoundDouble(double target, int number)
    {
        if(number == 3)
        {
            return Math.round(target*100)/100.0; //소수점 세번째자리에서 반올림
        }
        else if(number ==4)
        {
            return Math.round(target*1000)/1000.0; //소수점 네번째자리에서 반올림
        }
        else if(number ==2)
        {
            return Math.round(target*10)/10.0; //소수점 두번째자리에서 반올림
        }
        else if(number ==5)
        {
            return Math.round(target*10000)/10000.0; //소수점 다섯번째자리에서 반올림
        }
        else
            return Math.round(target);//첫번째 자리에서 반올림
    }


/*    다음 회차계산방법 (이전 회차의 결과로 계산)
     - 총매입액*40 / 시드 (소수점 두번째 자리에서 반올림)
     - 총매입액 = 평단가*보유량 (소수점 두번째 자리에서 반올림)
 */
    public static double getRoundNum()//회차를 리턴
    {
        double purchaseMount =  getRoundDouble(investor.totalMount * investor.averagePrice,2);//총매입액
        double roundNum = getRoundDouble(purchaseMount* investor.division / investor.seedMoney,2);//회차계산
        return roundNum;
    }

    public static void saveDB(Summary summary)
    {
        String sql = "insert into lao_test_result(symbol, division,fail_count, earning_rate,standard_year)" +
                "values(?,?,?,?,?)";
        try {
            Class.forName(DRIVER); // 1. 드라이버 로딩 (이름으로 클래스 가져오기)
            if(conn == null)
            {
                conn = DriverManager.getConnection(URL,USER,PASS); // 연결
            }
            ps = conn.prepareStatement(sql);
            ps.setString(1,summary.symbol);
            ps.setInt(2,summary.division);
            ps.setInt(3,summary.failHistories.size());
            ps.setDouble(4,summary.incomePer);
            ps.setString(5,summary.year);
            int result = ps.executeUpdate();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void saveStock(String symbol)
    {
        try {
            Calendar from = Calendar.getInstance();
            Calendar to = Calendar.getInstance();
            from.add(Calendar.YEAR,-10);
            Stock stock = YahooFinance.get(symbol,from,to, Interval.DAILY);

            List<HistoricalQuote> stockHistory = stock.getHistory();
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");

            for(HistoricalQuote pastStock : stockHistory)
            {
                String standardDate = dateFormat.format(pastStock.getDate().getTime());
                double closePrice = getRoundDouble(pastStock.getClose().doubleValue(),3);
                double highPrice = getRoundDouble(pastStock.getHigh().doubleValue(),3);

                TargetStock targetStock = new TargetStock(symbol,standardDate,closePrice,highPrice);
                targetStocks.add(targetStock);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
