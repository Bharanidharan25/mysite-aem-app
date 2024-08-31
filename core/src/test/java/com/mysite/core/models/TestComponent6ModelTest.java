package com.mysite.core.models;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class TestComponent6ModelTest {


	@InjectMocks
    TestComponent6Model modelObj;

    @Test
    void testGettersAndSetters(){
	   modelObj = new TestComponent6Model(); 
       modelObj.setBharani("testValue"); 
       assertEquals("testValue",modelObj.getBharani()); 

    }


}
