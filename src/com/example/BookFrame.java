package com.example;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

public class BookFrame extends JFrame implements ActionListener {
    JButton btnInsert, btnSelect, btnUpdate, btnDelete;
    JTextField tfNo;
    JPanel pNorth, pCenter, pSouth;
    public BookFrame() {
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setBounds(400, 400, 400, 300); // X, Y, 가로 세로
        pNorth = new JPanel();
        pNorth.setBackground(Color.ORANGE);
        add(pNorth, BorderLayout.NORTH);
        btnInsert = new JButton("입력폼");
        btnSelect = new JButton("리스트");
        btnUpdate = new JButton("수정폼");
        btnDelete = new JButton("삭제");
        btnInsert.addActionListener(this);
        btnSelect.addActionListener(this);
        btnUpdate.addActionListener(this);
        btnDelete.addActionListener(this);
        tfNo = new JTextField(5);
        pNorth.add(btnInsert);
        pNorth.add(btnSelect);
        pNorth.add(tfNo);
        pNorth.add(btnUpdate);
        pNorth.add(btnDelete);
        tfNo.setEditable(false);
        btnUpdate.setEnabled(false);
        btnDelete.setEnabled(false);
        pCenter = new JPanel(new BorderLayout());
        pCenter.setBackground(Color.LIGHT_GRAY);
        add(pCenter, BorderLayout.CENTER);
        pSouth = new JPanel();
        pSouth.setBackground(Color.PINK);
        add(pSouth, BorderLayout.SOUTH);
        setVisible(true);
    }// BookFrame 생성자
    class PaneInsert extends JPanel {
        JPanel piWest, piCenter, piSouth;
        JTextField tfTitle, tfAuthor, tfPrice, tfCategory, tfPublisher, tfPubdate;
        JButton btnPiInsert;
        public PaneInsert() {
            setLayout(new BorderLayout());
            piWest = new JPanel(new GridLayout(6, 1));
            add(piWest, BorderLayout.WEST);
            piWest.add(new JLabel("제목"));
            piWest.add(new JLabel("저자"));
            piWest.add(new JLabel("가격"));
            piWest.add(new JLabel("카테고리"));
            piWest.add(new JLabel("출판사"));
            piWest.add(new JLabel("출판일"));
            piCenter = new JPanel(new GridLayout(6, 1));
            add(piCenter, BorderLayout.CENTER);
            tfTitle = new JTextField();
            tfAuthor = new JTextField();
            tfPrice = new JTextField();
            tfCategory = new JTextField();
            tfPublisher = new JTextField();
            tfPubdate = new JTextField();
            piCenter.add(tfTitle);
            piCenter.add(tfAuthor);
            piCenter.add(tfPrice);
            piCenter.add(tfCategory);
            piCenter.add(tfPublisher);
            piCenter.add(tfPubdate);
            piSouth = new JPanel();
            add(piSouth, BorderLayout.SOUTH);
            btnPiInsert = new JButton("저장하기");
            piSouth.add(btnPiInsert);
            setVisible(true);
        }// PaneInsert()
    }// class PaneInsert end
    public static void main(String[] args) {
        BookFrame f = new BookFrame();
    }// main
    @Override
    public void actionPerformed(ActionEvent e) {
        System.out.println(e);
    }// BookFrame actionPerformed
}// end
