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

package org.funbizmodel.bookstore.model.author;

import org.funbizmodel.bookstore.model.book.BookContext;

import java.util.function.Function;
import java.util.stream.Stream;

/**
 * @author Carlos Sierra Andrés
 */
public class AuthorBuilder {
	protected String name;
	protected Stream<BookContext> books = Stream.empty();

	public AuthorBuilder name(String name) {
		this.name = name;return this;
	}

	public AuthorBuilder books(Stream<BookContext> books) {

		this.books = books;

		return this;
	}
}
