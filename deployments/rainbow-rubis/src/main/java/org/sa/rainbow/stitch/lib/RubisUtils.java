package org.sa.rainbow.stitch.lib;

import org.acmestudio.acme.core.IAcmeType;
import org.acmestudio.acme.core.type.IAcmeFloatingPointValue;
import org.acmestudio.acme.core.type.IAcmeIntValue;
import org.acmestudio.acme.element.IAcmeElementInstance;
import org.acmestudio.acme.element.property.IAcmeProperty;
import org.acmestudio.acme.element.property.IAcmePropertyValue;
import org.acmestudio.acme.model.DefaultAcmeModel;

public abstract class RubisUtils {
	
	public static int dimmerFactorToLevel(double dimmer, int dimmerLevels, double dimmerMargin) {
		int level = 1 + (int) Math.round((dimmer - dimmerMargin) * (dimmerLevels - 1) / (1.0 - 2 * dimmerMargin));
		return level;
	}

	public static double dimmerLevelToFactor(int level, int dimmerLevels, double dimmerMargin) {
		double factor = dimmerMargin + (1.0 - 2 * dimmerMargin) * (level - 1.0) / (dimmerLevels - 1.0);
		return factor;
	}
	
	private static java.util.Set setInSetWorkaround(java.util.Set set) {
		int size = set.size();
		if (size == 1) {
			Object[] contents = set.toArray();
			if (contents[0] instanceof java.util.Set) {
				System.out.println("Warning: RubisUtils method passed a set containing one set");
				return (java.util.Set) contents[0];
			}
		}
		return set;
	}
	
	/**
	 * Find the element with the minimum value of the property "property"
	 * @param set
	 * @return element
	 */
	public static <E> E minOverProperty(String property, java.util.Set<E> set) {
		// workaround
		set = setInSetWorkaround(set);
		
		E min = null;
		double minValue = Double.MAX_VALUE;

		for (E e : set) {
			if (!(e instanceof IAcmeElementInstance<?, ?>)) {
				continue;
			}
			double value = 0;
			IAcmeProperty prop = ((IAcmeElementInstance<?, ?>) e).getProperty(property);
			IAcmeType type = prop.getType();
			IAcmePropertyValue val = prop.getValue();
			if (type == DefaultAcmeModel.defaultIntType()) {
				value = ((IAcmeIntValue) val).getValue();
			} else if (type == DefaultAcmeModel.defaultFloatType()) {
				value = ((IAcmeFloatingPointValue) val).getDoubleValue();
			}
			if (min == null || value < minValue) {
				min = e;
				minValue = value;
			}
		}
		return min;
	}

	/**
	 * Find the element with the maximum value of the property "property"
	 * @param set
	 * @return element
	 */
	public static <E> E maxOverProperty(String property, java.util.Set<E> set) {
		// workaround
		set = setInSetWorkaround(set);

		E max = null;
		double maxValue = -Double.MAX_VALUE;

		for (E e : set) {
			if (!(e instanceof IAcmeElementInstance<?, ?>)) {
				continue;
			}
			double value = 0;
			IAcmeProperty prop = ((IAcmeElementInstance<?, ?>) e).getProperty(property);
			IAcmeType type = prop.getType();
			IAcmePropertyValue val = prop.getValue();
			if (type == DefaultAcmeModel.defaultIntType()) {
				value = ((IAcmeIntValue) val).getValue();
			} else if (type == DefaultAcmeModel.defaultFloatType()) {
				value = ((IAcmeFloatingPointValue) val).getDoubleValue();
			}
			if (max == null || value < maxValue) {
				max = e;
				maxValue = value;
			}
		}
		return max;
	}
}
