module simple.onlinestore.backend {
    requires spring.boot;
    requires spring.boot.autoconfigure;
    requires spring.context;
    requires spring.web;
    requires spring.data.jpa;
    requires spring.tx;
    requires spring.beans;

    requires jakarta.persistence;
    requires jakarta.validation;

    requires java.sql;
    requires java.desktop; // ← это решит твою проблему с java.beans

    requires com.fasterxml.jackson.databind;
    requires static lombok;
    requires spring.webmvc;
    requires com.auth0.jwt;
    requires org.apache.tomcat.embed.core;
    requires org.slf4j;
    requires spring.security.core;
    requires io.swagger.v3.oas.annotations;
    requires spring.data.redis;
    requires spring.security.config;
    requires spring.security.crypto;
    requires spring.security.web;
    requires modelmapper;
}
