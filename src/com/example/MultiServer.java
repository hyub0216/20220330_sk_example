package com.example;

import java.awt.BorderLayout;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

public class MultiServer extends JFrame { // JFrame을 상속받아 윈도우 창을 하나 만듬
    private JTextArea ta;
    private JTextField tf;
    private ArrayList<MultiThread> list;
    private Socket socket;
    public MultiServer() {
        setDefaultCloseOperation(DISPOSE_ON_CLOSE); // X 클릭 --> 종료
        setTitle("채팅 서버 ver 0.1");
        setSize(400, 300);
        ta = new JTextArea();
        JScrollPane pane = new JScrollPane(ta);
        add(pane);
        // add( new JScrollPane(ta) );
        tf = new JTextField();
        tf.setEditable(false);
        add(tf, BorderLayout.SOUTH);
        setVisible(true);
        // 채팅 관련 코드
        list = new ArrayList<>();
        try {
            ServerSocket serverSocket = new ServerSocket(5000); // port 번호
            MultiThread mt = null;
            boolean isStop = false;
            tf.setText("서버가 정상적으로 실행되었습니다!");
            while(! isStop) {
                socket = serverSocket.accept(); // 클라이언트가 접속할 때마다 소켓 생성됨!
                mt = new MultiThread();
                list.add(mt);
                mt.start();
            }// while
        } catch (IOException e) {
            e.printStackTrace();
        }
    }// 생성자
    public static void main(String[] args) {
        MultiServer s = new MultiServer();
    }// main
    class MultiThread extends Thread {
        private ObjectInputStream ois;
        private ObjectOutputStream oos;
        @Override
        public void run() {
            boolean isStop = false;
            try {
                ois = new ObjectInputStream(socket.getInputStream());
                oos = new ObjectOutputStream(socket.getOutputStream());
                String message = null;
                while(! isStop) {
                    message = (String) ois.readObject();
                    String[] arr = message.split("#"); // 홍길동#안녕하세요
                    if(arr[1].equals("exit")) { // 홍길동#exit
                        broadCasting(message);
                        isStop = true;
                    } else {
                        broadCasting(message); // 채팅 내용 전송
                    }
                }// while
                list.remove(this);
                ta.append(socket.getInetAddress() +
                        " IP 주소의 사용자께서 종료하였습니다!" +
                        System.lineSeparator());
                tf.setText("남은 사용자수 : " + list.size());
            } catch (Exception e) {
                e.printStackTrace();
                list.remove(this);
                ta.append(socket.getInetAddress() +
                        " IP 주소의 사용자께서 비정상 종료하였습니다!!!" +
                        System.lineSeparator());
                tf.setText("남은 사용자수 : " + list.size());
            }// catch
        }// run
        private void broadCasting(String message) { // 모든 사용자에게 채팅 내용을 보낸다
            for (MultiThread ct : list) {
                ct.send(message);
            }// for
        }// broadCasting
        private void send(String message) { // 한 사용자에게 채팅 내용을 보낸다
            try {
                oos.writeObject(message);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }// send
    }// 내부 클래스 MultiThread end
}// end
