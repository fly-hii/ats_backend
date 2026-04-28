package com.example.ATSCIRCLE.Exception;



public class UserLimitExceededException extends RuntimeException {
    public UserLimitExceededException(String message) {
        super(message);
    }
}
