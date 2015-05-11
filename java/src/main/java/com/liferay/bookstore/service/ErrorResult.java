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

package com.liferay.bookstore.service;

import java.util.ArrayList;
import java.util.List;

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

	public void addError(String error) {
		_errors.add(error);
	}

	@Override
	public String toString() {
		return "Errors: " + getErrors();
	}
}
