package com.woowa.trafficTest.Entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "user_data")
public class User {

    @Id @GeneratedValue
    private Long id;

    private String name;

    public String makeLog(){
        return  "Processing User ID: " + id
                + " Name: " + name
                + " UUID: " + UUID.randomUUID().toString();
    }
}
