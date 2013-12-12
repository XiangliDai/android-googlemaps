package com.example.hellogooglemaps;

import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.junit.Test;
import java.util.List;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;
@RunWith(RobolectricGradleTestRunner.class)
public class DummyTest {

    @Test
    public void testSomething() throws Exception {
        //mock creation
        List mockedList = mock(List.class);

        //using mock object
        mockedList.add("one");
        mockedList.clear();

        //verification
        verify(mockedList).add("one");
        verify(mockedList).clear();

    }
}
