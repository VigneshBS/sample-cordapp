package com.template.contracts;

import com.template.states.IOUState;
import org.junit.Test;

public class StateTests {

    //Mock State test check for if the state has correct parameters type
    @Test
    public void hasFieldOfCorrectType() throws NoSuchFieldException {
        IOUState.class.getDeclaredField("msg");
        assert (IOUState.class.getDeclaredField("msg").getType().equals(String.class));
    }
}