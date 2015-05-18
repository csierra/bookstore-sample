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

package org.funbizmodel.bookstore.model.book;

import java.sql.ResultSet;
import java.util.Optional;
import java.util.Spliterator;
import java.util.function.Consumer;

/**
* @author Carlos Sierra Andrés
*/
class BookContextSpliterator
	implements Spliterator<BookContext> {

	private BookService _bookService;
	private final ResultSet _resultSet;

	public BookContextSpliterator(BookService bookService, ResultSet resultSet) {
		_bookService = bookService;
		_resultSet = resultSet;
	}

	@Override
	public boolean tryAdvance(Consumer<? super BookContext> action) {
		Optional<BookQuerier> maybeQuerier =
			BookQuerier.fromResultSet(
				_bookService._authorService, _bookService, _resultSet);

		maybeQuerier.ifPresent(
			querier -> action.accept(
				new BookService.BookContextFromQuerier(querier)));

		return maybeQuerier.isPresent();
	}

	@Override
	public Spliterator<BookContext> trySplit() {
		return null;
	}

	@Override
	public long estimateSize() {
		return Long.MAX_VALUE;
	}

	@Override
	public int characteristics() {
		return IMMUTABLE;
	}
}
