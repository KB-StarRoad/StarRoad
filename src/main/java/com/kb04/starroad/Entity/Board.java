package com.kb04.starroad.Entity;

import lombok.*;

import javax.persistence.*;
import java.util.Date;

    @Entity
    @Getter
    @Table(name = "board")
    @AllArgsConstructor
    @NoArgsConstructor

    public class Board {
        @Id
        @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "board_seq")
        @SequenceGenerator(name = "board_seq", sequenceName = "BOARD_SEQ", allocationSize = 1)
        @Column(name = "no")
        private int no;

        @ManyToOne(fetch = FetchType.EAGER)
        @JoinColumn(name = "member_no")
        private Member memberNo;

        @Column(name = "title", length = 50, nullable = false)
        private String title;

        @Column(name = "regdate", nullable = false)
        private Date regdate;

        @PrePersist
        protected void onCreate() {
            regdate = new Date(); // 현재 날짜와 시간을 설정

            // status 필드가 null인 경우 '1'로 초기화
            if (status == null) {
                status = '1';
            }


        }

        @Column(name = "content", length = 2000, nullable = false)
        private String content;

        @Column(name = "likes")
        private int likes;

        @Column(name = "comment_num", nullable = false)
        private int commentNum = 0;

        @Column(columnDefinition = "char(1)  default '1'", name = "status", nullable = false)
        private Character status;



        @Column(name = "type", length = 1, nullable = false)
        private String type;

        @Lob
        @Column(name = "image")
        private byte[] image;

        @Column(name = "detail_type", length = 100, nullable = false)
        private String detailType;
    }