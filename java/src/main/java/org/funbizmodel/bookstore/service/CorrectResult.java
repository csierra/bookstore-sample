/**
 * Copyright (c) 2000-present Liferay, Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

package org.funbizmodel.bookstore.service;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * @author Carlos Sierra Andrés
 */
public class CorrectResult<R> implements Result<R> {

	private R _result;

	public CorrectResult(R result) {

		_result = result;
	}

	@Override
	public List<String> getErrors() {
		return Collections.emptyList();
	}

	@Override
	public R get() {
		return _result;
	}

	@Override
	public Result<R> andThen(Consumer<R> consumer) {
		consumer.accept(_result);

		return this;
	}

	@Override
	public Result<R> orElse(Consumer<List<String>> errors) {
		return this;
	}

	@Override
	public R getOrElse(Function<List<String>, R> function) {
		return get();
	}

	@Override
	public String toString() {
		return "Result: " + _result.toString();
	}
}
