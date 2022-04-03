package com.example;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.charset.UnsupportedCharsetException;

public class Memo extends JFrame implements ActionListener {

    JTextArea ta;
    JMenuBar bar;
    JMenu fileMenu, editMenu, formMenu, viewMenu, helpMenu;
    JMenuItem itemNew, itemOpen, itemSave, itemExit;

    public Memo(){

        //창을 닫으면 서비스가 종료된다.
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setTitle("sk_example_20220330");
        //크기지정
        setSize(400,300);
        bar = new JMenuBar();
        setJMenuBar(bar);
        fileMenu = new JMenu("파일");
        editMenu = new JMenu("편집");
        formMenu = new JMenu("서식");
        viewMenu = new JMenu("보기");
        helpMenu = new JMenu("도움말");
        bar.add(fileMenu);
        bar.add(editMenu);
        bar.add(formMenu);
        bar.add(viewMenu);
        bar.add(helpMenu);

        itemNew = new JMenuItem("새로만들기");
        itemOpen = new JMenuItem("열기");
        itemSave = new JMenuItem("저장");
        itemExit = new JMenuItem("종료");
        fileMenu.add(itemNew);
        fileMenu.add(itemOpen);
        fileMenu.add(itemSave);
        fileMenu.addSeparator();
        fileMenu.add(itemExit);

        // 리스너를 붙인다. (스위치를 붙인다.)
        itemNew.addActionListener(this);
        itemOpen.addActionListener(this);
        itemSave.addActionListener(this);
        itemExit.addActionListener(this);

        ta = new JTextArea();
        JScrollPane pane = new JScrollPane(ta);
        add(pane);

        //보이기
        setVisible(true);

    }//Default Constructor

    public static void main(String[] args) {
        Memo m = new Memo();
    }

//    INPUTSTREAM 1BYTE , BUFFEREDREADER 한줄씩 읽어서 성능이 향상됨

    private void openFile(){
        JFileChooser jFileChooser = new JFileChooser();
        int returnVal = jFileChooser.showOpenDialog(this); // Memo에 띄울꺼다.

        if(returnVal == JFileChooser.APPROVE_OPTION){//열기 = 0;

            File file = jFileChooser.getSelectedFile();// 선택한 파일이 리턴됨

            BufferedReader bufferedReader = null;// 한줄읽기 쌉가능 ; 메소드에 선언된 변수는 초기화를 해줘야함
            InputStreamReader inputStreamReader = null;
            FileInputStream fileInputStream = null;

            try{
                fileInputStream = new FileInputStream(file); //파일로 먼저 읽고
                inputStreamReader = new InputStreamReader(fileInputStream, StandardCharsets.UTF_8); // 캐릭터 인코딩설정
                bufferedReader = new BufferedReader(inputStreamReader); // byte -> 2byte -> 한줄 읽을때 처음시작은 byte로 읽음

                String str = "";
                while((str = bufferedReader.readLine()) != null)
                {
                    ta.append(str+System.lineSeparator()); // Win: \n\r, Linux : \n 을 자동으로 맞춰줌
                }
            }catch (FileNotFoundException e) //fileInputStream 의 예외처리
            {
                e.printStackTrace();

            }catch (UnsupportedCharsetException e) //inputStreamReader 의 예외처리
            {
                e.printStackTrace();
            }catch (IOException e)// readLine() 의 예외처리
            {
                e.printStackTrace();;
            }finally {

                //열었던것을 다시 순서대로 Close 해야 함
                //try-with-resource를 사용해도됨
                if(fileInputStream != null)
                {
                    try {
                        fileInputStream.close();//한곳에서 모든 것을 close하면 안되는 이유가 앞에 꺼가 안되면 나머지도 수행이안되버림
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                if(inputStreamReader != null)
                {
                    try {
                        inputStreamReader.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                if(bufferedReader != null)
                {
                    try {
                        bufferedReader.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {

        if(e.getSource() == itemNew)
        {
            /*기능넣기*/
        }
        if(e.getSource() == itemOpen)
        {
            openFile();
        }
        if(e.getSource() == itemSave)
        {
            saveFile();
        }
        if(e.getSource() == itemExit)
        {
            dispose();
        }
    }

    private void saveFile() {
//        JFileChooser
    }
}
