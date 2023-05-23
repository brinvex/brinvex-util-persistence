package com.brinvex.util.persistence.dba.common;

import java.io.Serializable;
import java.util.Objects;
import java.util.StringJoiner;

public class OsCmdResult implements Serializable {

    private final String out;

    private final String err;

    public OsCmdResult(String out, String err) {
        this.out = out;
        this.err = err;
    }

    public String getOut() {
        return out;
    }

    public String getErr() {
        return err;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OsCmdResult that = (OsCmdResult) o;
        return Objects.equals(out, that.out) && Objects.equals(err, that.err);
    }

    @Override
    public int hashCode() {
        return Objects.hash(out, err);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", OsCmdResult.class.getSimpleName() + "[", "]")
                .add("out='" + out + "'")
                .add("err='" + err + "'")
                .toString();
    }
}
