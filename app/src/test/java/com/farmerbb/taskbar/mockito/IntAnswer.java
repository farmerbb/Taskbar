package com.farmerbb.taskbar.mockito;

import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

public class IntAnswer implements Answer<Integer> {
    public int answer = 0;

    @Override
    public Integer answer(InvocationOnMock invocation) {
        return answer;
    }
}
