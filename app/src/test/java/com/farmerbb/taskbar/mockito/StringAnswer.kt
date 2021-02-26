package com.farmerbb.taskbar.mockito

import org.mockito.invocation.InvocationOnMock
import org.mockito.stubbing.Answer

class StringAnswer(answerValue: String) : Answer<String> {
    private var answer: String = answerValue

    @Throws(Throwable::class)
    override fun answer(invocation: InvocationOnMock): String {
        return answer
    }
}
