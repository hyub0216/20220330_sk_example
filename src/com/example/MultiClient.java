package com.example;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

public class MultiClient extends JFrame implements ActionListener, Runnable {
    private JTextArea ta;
    private JTextField tf;
    private JLabel label1, label2;
    private JPanel pSouth, pNorth;
    private String ip;
    private String id;
    private JButton btnExit;
    private Socket socket;
    private ObjectInputStream ois;
    private ObjectOutputStream oos;
    public MultiClient(String serverIp, String name) {
//        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(400, 300);
        ip = serverIp;
        id = name;
        // 북쪽(위쪽)에 붙을 판넬
        pNorth = new JPanel(new BorderLayout());
        label1 = new JLabel("대화명 : [" + id + "]"); // 대화명 : [홍길동]
        label2 = new JLabel("IP 주소 : " + ip); // IP 주소 : 127.0.0.1
        pNorth.add(label1, BorderLayout.CENTER);
        pNorth.add(label2, BorderLayout.EAST); // 동쪽(오른쪽)
        add(pNorth, BorderLayout.NORTH);
        // 남쪽(아래쪽)에 붙을 판넬
        pSouth = new JPanel(new BorderLayout());
        tf = new JTextField(30);
        btnExit = new JButton("종료");
        pSouth.add(btnExit, BorderLayout.EAST);
        pSouth.add(tf, BorderLayout.CENTER);
        add(pSouth, BorderLayout.SOUTH);
        // Center(중앙)에 JTextArea 붙이기
        ta = new JTextArea();
        JScrollPane pane = new JScrollPane(ta);
        add(pane, BorderLayout.CENTER);
        // 리스너 등록
        tf.addActionListener(this); // 채팅 입력란에 리스너 등록
        btnExit.addActionListener(this); // this는 나 자신이 이벤트를 처리한다.
        setVisible(true);
        // 채팅 관련 코드
        try {
            socket = new Socket(ip, 5000);
            System.out.println("서버에 접속되었습니다!!!");
            oos = new ObjectOutputStream(socket.getOutputStream());
            oos.flush();
            ois = new ObjectInputStream(socket.getInputStream());
            Thread t = new Thread(this); // Runnable 구현했기 때문에 조수석에 탈 수 있다.
            t.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        //x를 클릭했을때 소켓을 닫고 종료하기
        addWindowListener(new WindowAdapter() { // 익명객체
            @Override
            public void windowClosing(WindowEvent e) {
                try {
                    oos.writeObject(id+"#"+"exit");
                } catch (IOException ioException) {
                    ioException.printStackTrace();
                } finally {
                    if(socket != null)
                    {
                        try {
                            socket.close();
                        } catch (IOException ioException) {
                            ioException.printStackTrace();
                        }
                    }
                }
            }
        });
        
    }// 생성자
    public static void main(String[] args) {
        MultiClient c1 = new MultiClient("127.0.0.1", "홍길동");
        MultiClient c2 = new MultiClient("127.0.0.1", "장길산");
    }// main
    @Override
    public void actionPerformed(ActionEvent e) {
        // System.out.println(e);
        if(e.getSource() == tf) {
            String msg = tf.getText();
            if(msg == null || msg.length() == 0) { // 내용이 없는 경우
                JOptionPane.showMessageDialog(this, "내용을 입력하세요",
                        "경고", JOptionPane.WARNING_MESSAGE);
            } else { // 내용이 있는 경우 채팅 내용 전송!!!
                try {
                    oos.writeObject(id + "#" + msg); // 홍길동#안녕
                    tf.setText("");
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        }// if
        if(e.getSource() == btnExit) {

            try {
                oos.writeObject(id+"#"+"exit");
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }finally {
                // oos, ois 연결 닫아주기.
                if(socket != null)
                {
                    try {
                        socket.close(); // socket만 닫아줘도 oos, ois다 종료됨
                    } catch (IOException ioException) {
                        ioException.printStackTrace();
                    }
                }
            }

        }
    }// actionPerformed
    @Override
    public void run() {
        String message = null;
        String[] receiveMsg = null;
        boolean isStop = false;
        while(! isStop) {
            try {
                message = (String) ois.readObject(); // 홍길동#안녕
                receiveMsg = message.split("#");
            } catch (ClassNotFoundException | IOException e) {
                e.printStackTrace();
                isStop = true;
            }
            String nick = receiveMsg[0];
            String cont = receiveMsg[1];
            System.out.println(nick + ":" + cont); // 홍길동#안녕
            if(cont.equals("exit")) {
                if(nick.equals(id)) { // exit한 사람이 나 자신이면
                    // System.exit(0); // 종료
                    // dispose();
                } else {
                    ta.append(nick + " 님이 종료했습니다!" + System.lineSeparator());
                    ta.setCaretPosition(ta.getDocument().getLength());
                }
            } else { // 채팅 내용이면
                ta.append(nick + " : " + cont + System.lineSeparator());
            }// else
        }// while
    }// run
}// end
