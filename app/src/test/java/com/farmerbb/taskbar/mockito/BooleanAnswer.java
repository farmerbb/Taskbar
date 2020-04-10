package com.farmerbb.taskbar.mockito;

import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

public class BooleanAnswer implements Answer<Boolean> {
    public boolean answer = false;

    @Override
    public Boolean answer(InvocationOnMock invocation) {
        return answer;
    }
}
