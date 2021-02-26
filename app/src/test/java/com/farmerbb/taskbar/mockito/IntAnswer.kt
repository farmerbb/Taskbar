package com.farmerbb.taskbar.mockito

import org.mockito.invocation.InvocationOnMock
import org.mockito.stubbing.Answer

class IntAnswer : Answer<Int> {
    var answer = 0
    override fun answer(invocation: InvocationOnMock): Int {
        return answer
    }
}
