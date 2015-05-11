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

package com.liferay.bookstore.model.book;

import com.liferay.bookstore.model.author.AuthorContext;

/**
 * @author Carlos Sierra Andrés
 */
public class BookBuilder {

	protected AuthorContext _authorContext;
	protected String _isbn;
	protected String _title;

	public BookBuilder isbn(String isbn) {
		_isbn = isbn;

		return this;
	}

	public BookBuilder title(String title) {
		_title = title;

		return this;
	}

	public BookBuilder setAuthor(AuthorContext authorContext) {
		_authorContext = authorContext;

		return this;
	}

}
