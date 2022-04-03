package com.example;


// 1. JDBC 드라이버 로딩
// 2. 연결
// 3. 명령 준비
// 4. 결과
// 5. 자원을 해제.

import javax.xml.crypto.Data;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDateTime;

public class JDBCtest1 {

    private static final String DRIVER ="com.mysql.cj.jdbc.Driver";
    private static final String URL ="jdbc:mysql://localhost:3306/sk_example?useSSL=false";
    private static final String USER = "root";
    private static final String PASS = "root";

    public static void main(String[] args) {
        Connection conn = null;
        PreparedStatement ps = null;
        String sql = "insert into book(title, author,price,category,publisher, pubdate)" +
                "values(?,?,?,?,?,?)";
        try {
            Class.forName(DRIVER); // 1. 드라이버 로딩 (이름으로 클래스 가져오기)
            conn = DriverManager.getConnection(URL,USER,PASS); // 연결
            ps = conn.prepareStatement(sql);
            ps.setString(1,"이펙티브자바");
            ps.setString(2,"이상협");
            ps.setInt(3,30000);
            ps.setString(4,"학술");
            ps.setString(5,"SK도서");
            java.util.Date date = new java.util.Date(2022,3,31);
            ps.setDate(6,new java.sql.Date(date.getTime()));
            int result = ps.executeUpdate();

            System.out.println("result : "+result );
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (SQLException e) {
            e.printStackTrace();
        }

    }
}//end
