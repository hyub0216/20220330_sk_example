package com.example;

import java.util.Date;
public class BookDTO {
    private int no;
    private String title;
    private String author;
    private int price;
    private String category;
    private String publisher;
    private Date pubdate;
    // 우클릭 > Source > Generate Getters and Setters > Select All > Generate
    public int getNo() {
        return no;
    }
    public void setNo(int no) {
        this.no = no;
    }
    public String getTitle() {
        return title;
    }
    public void setTitle(String title) {
        this.title = title;
    }
    public String getAuthor() {
        return author;
    }
    public void setAuthor(String author) {
        this.author = author;
    }
    public int getPrice() {
        return price;
    }
    public void setPrice(int price) {
        this.price = price;
    }
    public String getCategory() {
        return category;
    }
    public void setCategory(String category) {
        this.category = category;
    }
    public String getPublisher() {
        return publisher;
    }
    public void setPublisher(String publisher) {
        this.publisher = publisher;
    }
    public Date getPubdate() {
        return pubdate;
    }
    public void setPubdate(Date pubdate) {
        this.pubdate = pubdate;
    }
    // 우클릭 > Source > Generate toString
    @Override
    public String toString() {
        return "BookDTO [no=" + no + ", title=" + title + ", author=" + author + ", price=" + price + ", category="
                + category + ", publisher=" + publisher + ", pubdate=" + pubdate + "]";
    }
    public BookDTO() { } // 기본 생성자
    // 우클릭 > Source > Generate Constructor using Fields >
    public BookDTO(String title, String author, int price, String category, String publisher, Date pubdate) {
        this.title = title;
        this.author = author;
        this.price = price;
        this.category = category;
        this.publisher = publisher;
        this.pubdate = pubdate;
    }
}// end
//create table book(
//  no int auto_increment primary key,
//  title varchar(50) not null,
//  author varchar(50) not null,
//  price int not null,
//  category varchar(20) not null,
//  publisher varchar(50) not null,
//  pubdate date not null
//);

