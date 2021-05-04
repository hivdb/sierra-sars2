/*

    Copyright (C) 2017 Stanford HIVDB team

    Sierra is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Sierra is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package edu.stanford.hivdb.graphql;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Set;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.PropertyDataFetcher;
import graphql.schema.PropertyDataFetcherHelper;

public class ExtGraphQL {

	public static DataFetcher<Object> pipeLineDataFetcher = env -> {
		return env.getSource();
	};
	
	public static class ExtPropertyDataFetcher<T> extends PropertyDataFetcher<T> {

		private final String propertyName;

		public ExtPropertyDataFetcher(String propertyName) {
			super(propertyName);
			this.propertyName = propertyName;
		}

		@SuppressWarnings("unchecked")
		@Override
		public T get(DataFetchingEnvironment environment) {
      Object source = environment.getSource();
      if (source == null) {
          return null;
      }
			T result = (T) getPropertyViaMethod(propertyName, source, environment);
			result = postProcess(result, environment);
			if (result instanceof Set) {
				// cast all set to list
				result = (T) new ArrayList<>((Set<?>) result);
			}
			return (T) result;
		}

		/**
		 * This protected method provide subclasses ability to process
		 * the fetched object.
		 * @param object
		 * @return object
		 */
		protected T postProcess(T object, DataFetchingEnvironment environment) {
			return object;
		}

	}
	
	public static Object getPropertyViaMethod(String propertyName, Object source, DataFetchingEnvironment environment) {
		Object defaultMethod = PropertyDataFetcherHelper.getPropertyValue(
			propertyName, source, environment.getFieldType(), environment);
		if (defaultMethod != null) {
			return defaultMethod;
		}
		try {
			Method method = source.getClass().getMethod(propertyName);
      return method.invoke(source);
		} catch (NoSuchMethodException e) {
			if (propertyName == "text") {
				return getPropertyViaMethod("toString", source, environment);
			}
			return getPropertyViaField(source, propertyName);
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		} catch (InvocationTargetException e) {
			throw new RuntimeException(e);
		}	
	}
	
	private static Object getPropertyViaField(Object object, String propertyName) {
		try {
			Field field = object.getClass().getField(propertyName);
			return field.get(object);
		} catch (NoSuchFieldException e) {
			return null;
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}

}
