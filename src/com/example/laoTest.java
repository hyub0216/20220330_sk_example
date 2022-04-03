package com.example;

import yahoofinance.Stock;
import yahoofinance.YahooFinance;
import yahoofinance.histquotes.HistoricalQuote;
import yahoofinance.histquotes.Interval;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class laoTest {

         /*
        라오어 무한매수법
        1. 투자금액을 40분할한다.
        2. 3ETF 종목을 선정한다.
        3. 일 구매 금액은 투자금액 / 40  ex) 투자금액 : 10000달라 인경우 일 구매금액은 250달라
        4. 일 구매 금액의 절반으로는 평단가 LOC 구매, 나머지로는 평단가+5% LOC구매
        5. 보유량의 25% 는 +5% LOC 매도, 나머지 보유량은 +10%에 지정가 매도
        6. 20회 차부터는 매수 매도 방법이 변경됨
           -- 매수는 loc평단에만
           -- 매도는 loc평단에 25%
                    지정가 5%에 25%
                    지정가 10% 50%
        7. 40회차는 그냥 종가 매도

        input :
        1. startDate (입력기간 기준 하루도 안쉬고 1년동안 했을경우)
        2. totalMoney 다시 새로운 round가 시작되면 total = total + earnedMoney;
        3. 40회차때는 그냥 종가매도.
        3. target ETF
        */

    private static final String DRIVER ="com.mysql.cj.jdbc.Driver";
    private static final String URL ="jdbc:mysql://localhost:3306/sk_example?useSSL=false";
    private static final String USER = "root";
    private static final String PASS = "root";
    private static Connection conn = null;
    private static PreparedStatement ps = null;
    private static List<TargetStock> targetStocks = new ArrayList<>();

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

    static class Wallet{

        double startMoney; // 초기자금
        double totalMoney; // 토탈자금 : 새로운 시작이 시작됐을때 변경
        double dayLimitMoney; // 하루 살수있는 금액 : 새로운 시작이 시작됐을때 변경
        int totalCount; // 갖고있는 갯수
        double averagePrice;// 평가
        double buyMoney; //구매 총 금액
        double sellMoney; //판매 총 금액
        double buyMoeny2; //보정되지 않는 구매 총금액
        int buyCount;  //회차
        double totalValue; // 가치
        boolean isDone;//끝났으면 false로


        Wallet(double pStartMoney)
        {
            startMoney = pStartMoney;
            totalCount =0;
            dayLimitMoney = pStartMoney / 40;
            averagePrice = 0;
            buyMoney =0;
            sellMoney =0;
            totalValue = pStartMoney;
            totalMoney =pStartMoney;
            buyCount =0;
            sellMoney =0;
            buyMoeny2 =0;
            isDone = false;
        }

        void init(double pStartMoney)
        {
            totalCount =0;
            dayLimitMoney = pStartMoney / 40;
            averagePrice = 0;
            buyMoney =0;
            totalMoney =pStartMoney;
            totalValue = pStartMoney;
            buyCount =0;
            sellMoney =0;
            buyMoeny2 =0;
            isDone = true;
        }
    }

    private static Wallet wallet;

    public static void main(String[] args) {
        saveStock("TQQQ"); // DB에 1년치 TQQQ정보넣기

        double inputMoney = 40000; //4천만원

        int totalCycle =0;
        int failCount =0;
        System.out.println(inputMoney/40);

        wallet = new Wallet(inputMoney);
        for(TargetStock targetStock : targetStocks)
        {
            if(targetStock.standardDate.substring(0,4).equals("2018"))
            {
                wallet.buyCount++;
                double loc5 = wallet.averagePrice*1.05; //평단가*1.05
                double loc10 = wallet.averagePrice*1.1; //평단가*1.1
                double averagePrice = wallet.averagePrice; // 평단가
                int totalCount = wallet.totalCount;  //총보유수량
                double totalMoney = wallet.totalMoney + (averagePrice*totalCount);

                if(wallet.isDone)
                {
                    System.out.println();
                    System.out.println();
                    wallet.buyCount =1;
                    wallet.isDone = false;
                    totalCycle ++;
                }

                if(wallet.buyCount == 1) // 처음 시작할때
                {
                    wallet.dayLimitMoney = totalMoney /40; // 하루 구매가능액 초기화
                    int count = (int) (wallet.dayLimitMoney / targetStock.closePrice);
                    buy(count, targetStock.closePrice);
                }
                else if(isFirstHalf()) // 20회차 미만인경우
                {
                    // 총구매금액 - 총 판매금액 > 초기자금*.0.5

                    int dayBuyCount;
                    // LOC 5%매수, LOC평단가 매수
                    // 추가 구매했을때만 평단가가 바뀜
                    if(targetStock.closePrice <= wallet.averagePrice) // 종가가 내 평단가 이하인경우 매수
                    {
                        dayBuyCount =(int) (wallet.dayLimitMoney / targetStock.closePrice); //전체 갯수개 구매
                        if(dayBuyCount > 0)
                        {
                            buy(dayBuyCount, targetStock.closePrice);
                        }
                    }
                    else if(targetStock.closePrice > wallet.averagePrice && targetStock.closePrice <=loc5)
                    {   // 내평단가 < 종가  <= 내평단가*1.05
                        //절반구매
                        dayBuyCount= (int) ((wallet.dayLimitMoney / targetStock.closePrice) / 2);
                        if(dayBuyCount > 0)
                        {
                            buy(dayBuyCount,targetStock.closePrice);
                        }
                    }
                    else if(targetStock.closePrice > loc5)//매도 25% 매수가 발생할때 발생하지 않음.
                    {
                        //종가 > loc5
                        //25%매도하고끝
                        int sellCount  = Long.valueOf(Math.round(totalCount * 0.25)).intValue();
                        if(sellCount>0)
                        {
                            sell(sellCount,targetStock.closePrice,"전판전LOC5매도");
                        }
                    }

                    if(targetStock.highPrice >= loc10) // 매수가 발생해도 발생할수있음
                    {
                        // 장중고가 >= loc10
                        int sellCount  = Long.valueOf(Math.round(totalCount * 0.75)).intValue();
                        if(sellCount>0)
                        {
                          sell(sellCount,loc10, "전반전LOC10매도");
                        }

                        if(targetStock.closePrice > loc5) // 전부다 팔렸으므로 초기화
                        {
                            wallet.isDone = true;
                        }
                    }
                }
                else if(!isFirstHalf())//20회차부터 변경되는 로직;
                {
                    int dayBuyCount;

                    if(targetStock.closePrice <= averagePrice) //매수
                    {
                        // 종가 <= 내평단가
                        dayBuyCount =(int) (wallet.dayLimitMoney / targetStock.closePrice); //하루 구매갯수
                        if(dayBuyCount > 0)
                        {
                            buy(dayBuyCount,targetStock.closePrice);
                        }
                    }
                    else if(targetStock.closePrice > averagePrice) //매도
                    {
                        // 내 평단가 < 종가 < loc5
                        //25%매도하고끝
                        int sellCount  = Long.valueOf(Math.round(totalCount * 0.25)).intValue();
                        if(sellCount>0)
                        {
                            sell(sellCount,targetStock.closePrice,"후반전 평단가LOC매도");
                        }
                    }

                    if(targetStock.highPrice >= loc5 && targetStock.highPrice <loc10)
                    {
                        //  loc5 <= 장중고가 < loc10
                        //25%매도하고끝
                        int sellCount  = Long.valueOf(Math.round(totalCount * 0.25)).intValue();
                        if(sellCount>0)
                        {
                            sell(sellCount,loc5,"후반전 LOC5매도");
                        }
                    }
                    else if(targetStock.highPrice >= loc10)
                    {
                        //전체매도
                        int loc10sellCount  = Long.valueOf(Math.round(totalCount * 0.5)).intValue();
                        int loc5sellCount  = Long.valueOf(Math.round(totalCount * 0.25)).intValue();
                        if(loc10sellCount>0)
                        {
                            sell(loc10sellCount,loc10,"후반전 LOC10매도")   ;
                        }

                        if(loc5sellCount>0)
                        {
                            sell(loc5sellCount,loc5,"후반전 LOC5매도")   ;
                        }

                        if(targetStock.closePrice > averagePrice && targetStock.closePrice <loc5)
                        {
                            wallet.isDone = true;
                        }
                    }
                }

                if(wallet.buyCount ==40)
                {
                    if(totalCount > 0)
                    {
                        if(wallet.averagePrice > targetStock.closePrice)
                        {
                            failCount++;
                        }
                        sell(totalCount,targetStock.closePrice,"40회차 매도");
                    }
                    wallet.isDone = true;
                }

                System.out.println("날짜 : "+targetStock.standardDate+ "-"+wallet.buyCount+"회차 : "
                        +"총금액 : "+wallet.totalValue
//                        +", 구매금액 : "+wallet.buyMoney
//                        +", 잔여금액 : "+wallet.totalMoney
//                        +" , 현재가치 : "+(targetStock.closePrice* wallet.totalCount)
                        +" , 보유수량 : "+wallet.totalCount
                        +" , 평단가 : "+wallet.averagePrice
                        +" , 종가 :  "+targetStock.closePrice
                        +" , 고가 :  "+targetStock.highPrice);
                if(wallet.isDone || wallet.totalCount ==0){
                    wallet.isDone =true;
                    wallet.init(wallet.totalValue);
                }
            }
        }
        double incomePercent = (wallet.totalValue / wallet.startMoney)*100;
        System.out.println("startMoney : "+wallet.startMoney);
        System.out.println("endMoney : "+wallet.totalValue);
        System.out.println("cycleCount : "+totalCycle);
        System.out.println("손절 횟수 : "+failCount);
        System.out.println("수익률 : "+incomePercent);

    }

    private static void sell(int count, double price, String coCl) {
        double daySellMoney = count*price; //판매금액
        wallet.totalCount -=count; //총 보유수량 감소
        wallet.buyMoney -= (wallet.averagePrice*count); // 총 구매금액 감소
        // 단순 판매라 평단가 영향 없음
        wallet.totalMoney += daySellMoney; // 남은 잔고 증가
        wallet.totalValue = wallet.totalMoney + (wallet.averagePrice* wallet.totalCount);
        wallet.sellMoney +=count*wallet.averagePrice;
        double incomeMoney = (price - wallet.averagePrice) * count;
        System.out.println("#"+coCl+" sell count : "+count+ " , price : "+price+" , 수익금액 : "+incomeMoney+", 현재가치  : "+wallet.totalValue);
    }

    private static void buy(int count, double price) {
//        System.out.println("buy count : "+count+ " , price : "+price);
        double dayBuyMoney = count*price; // 구매금액
        wallet.buyMoeny2 += dayBuyMoney;
        wallet.totalCount += count; //총 보유량 증가
        wallet.buyMoney +=dayBuyMoney; //총 구매금액 증가
        wallet.averagePrice = wallet.buyMoney / wallet.totalCount; //평단가 변경
        wallet.totalMoney -= dayBuyMoney; //남은 잔고변경
        wallet.totalValue = wallet.totalMoney + (wallet.averagePrice* wallet.totalCount);
    }

    public static boolean isFirstHalf()
    {
        System.out.println("###구매금액 : "+wallet.buyMoeny2+", 매도금액 : "+wallet.sellMoney+", 초기자금 1/2 : "+wallet.startMoney*0.5);
        if(wallet.buyMoeny2 - wallet.sellMoney < wallet.startMoney*0.5)
        {
            return true;
        }
        else
            return false;
    }

/*    public static void saveDB(String symbol, String standardDate, double closePrice, double highPrice)
    {
        String sql = "insert into stock(symbol, standard_date,close_price,high_price)" +
                "values(?,?,?,?)";
        try {
            Class.forName(DRIVER); // 1. 드라이버 로딩 (이름으로 클래스 가져오기)
            if(conn == null)
            {
                conn = DriverManager.getConnection(URL,USER,PASS); // 연결
            }
            ps = conn.prepareStatement(sql);
            ps.setString(1,symbol);
            ps.setString(2,standardDate);
            ps.setDouble(3,closePrice);
            ps.setDouble(4,highPrice);
            int result = ps.executeUpdate();
            System.out.println("result : "+result );
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }*/

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
                double closePrice = pastStock.getClose().doubleValue();
                double highPrice = pastStock.getHigh().doubleValue();

//                saveDB(symbol,standardDate,closePrice,highPrice);
                TargetStock targetStock = new TargetStock(symbol,standardDate,closePrice,highPrice);
                targetStocks.add(targetStock);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
