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

package com.liferay.bookstore.model.author;

import com.liferay.bookstore.model.book.BookQuerier;
import com.liferay.bookstore.service.ReadOnlyContext;

import java.util.stream.Stream;

/**
 * @author Carlos Sierra Andrés
 */
public interface AuthorQuerier {
	public String id();

	public String name();

	public <R> Stream<? extends ReadOnlyContext<BookQuerier>> books();

}
