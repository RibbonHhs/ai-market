package com.meiya.skillsmap.common;

public class BizException extends RuntimeException {

    private final int code;

    public BizException(BizCode bizCode) {
        super(bizCode.getMessage());
        this.code = bizCode.getCode();
    }

    public BizException(BizCode bizCode, String message) {
        super(message);
        this.code = bizCode.getCode();
    }

    public BizException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int getCode() { return code; }
}
