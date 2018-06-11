package org.mcv.mu;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.mcv.math.BigInteger;

import lombok.experimental.Delegate;

public class InfiniteList implements List<BigInteger> {

	BigInteger start;
	BigInteger next;
	boolean down;
	boolean first;
	
	InfiniteList(BigInteger start, boolean down) {
		this.start = start;
		this.down = down;
	}
	
	private static interface ListGet {
		Iterator<BigInteger> iterator();
	}
	
	@Delegate(excludes=ListGet.class) List<BigInteger> list = new ArrayList<>();
	
	@Override
	public Iterator<BigInteger> iterator() {

		first = true;
		
		return new Iterator<BigInteger>() {

			@Override
			public boolean hasNext() {
				return true;
			}

			@Override
			public BigInteger next() {
				if(first) {
					first = false;
					next = start;
					return start;
				}
				if(down) next = next.subtract(BigInteger.ONE);
				else next = next.add(BigInteger.ONE);
				return next;
			}			
		};
	}
}
