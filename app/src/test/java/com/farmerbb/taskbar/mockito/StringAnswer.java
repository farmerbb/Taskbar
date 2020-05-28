package com.farmerbb.taskbar.mockito;

import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

public class StringAnswer implements Answer<String> {
    public String answer = null;

    @Override
    public String answer(InvocationOnMock invocation) throws Throwable {
        return answer;
    }
}
