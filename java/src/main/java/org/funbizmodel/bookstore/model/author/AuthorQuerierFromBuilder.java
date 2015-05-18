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

import org.funbizmodel.bookstore.model.book.BookQuerier;
import org.funbizmodel.bookstore.service.ReadOnlyContext;

import java.util.stream.Stream;

/**
* @author Carlos Sierra Andrés
*/
class AuthorQuerierFromBuilder implements AuthorQuerier {

	private AuthorService _authorService;
	private long _id;
	private AuthorBuilder _authorBuilder;

	public AuthorQuerierFromBuilder(AuthorService authorService,
									long id, AuthorBuilder authorBuilder) {
		_authorService = authorService;

		_id = id;
		_authorBuilder = authorBuilder;
	}

	@Override
	public String id() {
		return Long.toString(_id);
	}

	@Override
	public String name() {
		return _authorBuilder.name;
	}

	@Override
	public Stream<? extends ReadOnlyContext<BookQuerier>> books() {
		return _authorBuilder.books;
	}
}
