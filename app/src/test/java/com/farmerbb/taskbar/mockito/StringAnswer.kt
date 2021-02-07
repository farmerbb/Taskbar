package com.farmerbb.taskbar.mockito

import org.mockito.invocation.InvocationOnMock
import org.mockito.stubbing.Answer

class StringAnswer : Answer<String?> {
    var answer: String? = null

    @Throws(Throwable::class)
    override fun answer(invocation: InvocationOnMock): String? {
        return answer
    }
}