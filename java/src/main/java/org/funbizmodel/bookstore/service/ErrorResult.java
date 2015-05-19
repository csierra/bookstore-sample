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

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * @author Carlos Sierra Andrés
 */
public class ErrorResult<R> implements Result<R> {

	List<String> _errors = new ArrayList<>();

	@Override
	public List<String> getErrors() {
		return _errors;
	}

	@Override
	public R get() {
		throw new RuntimeException(_errors.toString());
	}

	@Override
	public Result<R> andThen(Consumer<R> consumer) {
		return this;
	}

	@Override
	public Result<R> orElse(Consumer<List<String>> consumer) {
		return this;
	}

	@Override
	public R getOrElse(Function<List<String>, R> function) {
		return function.apply(_errors);
	}

	public void addError(String error) {
		_errors.add(error);
	}

	@Override
	public String toString() {
		return "Errors: " + getErrors();
	}
}
