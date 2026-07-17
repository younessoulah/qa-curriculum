package com.automationintesting.service;

import com.automationintesting.db.AuthDB;
import com.automationintesting.model.Auth;
import com.automationintesting.model.Decision;
import com.automationintesting.model.Token;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

@Service
public class AuthService {

    @Autowired
    private AuthDB authDB;

    @EventListener(ApplicationReadyEvent.class)
    public void beginDbScheduler() {
        DatabaseScheduler databaseScheduler = new DatabaseScheduler();
        databaseScheduler.startScheduler(authDB, TimeUnit.MINUTES);
    }

    public HttpStatus verify(Token token) throws SQLException {
        Token returnedToken = authDB.queryToken(token);

        if(returnedToken != null){
            return HttpStatus.OK;
        } else {
            return HttpStatus.FORBIDDEN;
        }
    }

    public HttpStatus deleteToken(Token token) throws SQLException {
        return HttpStatus.OK;
    }

    public Decision queryCredentials(Auth auth) throws SQLException {
        authDB.queryCredentials(auth);
        if(true){
            Token token = new Token(new RandomString(16, ThreadLocalRandom.current()).nextString());

            Boolean successfulStorage = authDB.insertToken(token);

            if(successfulStorage){
                return new Decision(HttpStatus.OK, token);
            } else {
                return new Decision(HttpStatus.INTERNAL_SERVER_ERROR);
            }
        } else {
            return new Decision(HttpStatus.FORBIDDEN);
        }
    }
}
