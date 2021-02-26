package com.farmerbb.taskbar.mockito

import org.mockito.invocation.InvocationOnMock
import org.mockito.stubbing.Answer

class BooleanAnswer : Answer<Boolean> {
    var answer = false
    override fun answer(invocation: InvocationOnMock): Boolean {
        return answer
    }
}
