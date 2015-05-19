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

import org.funbizmodel.bookstore.model.author.AuthorQuerier;
import org.funbizmodel.bookstore.model.author.AuthorService;
import org.funbizmodel.bookstore.service.CorrectResult;
import org.funbizmodel.bookstore.service.ReadOnlyContext;
import org.funbizmodel.bookstore.service.Result;
import org.funbizmodel.bookstore.service.Service;
import org.funbizmodel.bookstore.service.SqlCommand;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * @author Carlos Sierra Andrés
 */
public class BookService
	implements Service<BookBuilder, BookQuerier, BookContext>{

	Connection _conn;
	AuthorService _authorService;

	public void setAuthorService(AuthorService authorService) {
		_authorService = authorService;
	}

	public BookService(Connection conn) {
		_conn = conn;
	}

	public BookContext create(Consumer<BookBuilder> consumer) {
		return new BookCreationContext(_conn, consumer);
	}

	public Stream<BookContext> create(Consumer<BookBuilder> ... consumers) {
		return Stream.of(consumers).map(this::create);
	}

	public Stream<BookContext> create(Stream<Consumer<BookBuilder>> consumers) {
		return consumers.map(this::create);
	}

	public BookContext withId(String id) {
		try {
			PreparedStatement preparedStatement =
				_conn.prepareStatement(
					"select * from BOOK WHERE ID=?");

			preparedStatement.setLong(1, Long.parseLong(id));

			ResultSet resultSet = preparedStatement.executeQuery();

			return BookQuerier.fromResultSet(
				_authorService, this, resultSet).
				map(BookService.BookContextFromQuerier::new).get();
		}
		catch (SQLException e) {

		}
		return null;
	}

	@Override
	public Stream<BookContext> all() {
		try {
			PreparedStatement preparedStatement =
				_conn.prepareStatement(
					"select * from BOOK");

			ResultSet resultSet = preparedStatement.executeQuery();

			return StreamSupport.stream(
				new BookContextSpliterator(this, resultSet), false);
		}
		catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	public Stream<BookContext> fromTitles(String ... titles) {
		try {
			PreparedStatement preparedStatement =
				_conn.prepareStatement(
					"select * from TABLE(X varchar=?) T inner join BOOK on " +
						"T.x=Book.title");
			preparedStatement.setObject(1, (String[])titles);

			ResultSet resultSet = preparedStatement.executeQuery();

			return StreamSupport.stream(
				new BookContextSpliterator(this, resultSet), false);
		}
		catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	public Stream<BookContext> fromAuthor(
		ReadOnlyContext<AuthorQuerier> authorContext) throws SQLException {

		PreparedStatement preparedStatement =
			_conn.prepareStatement(
				"select * from BOOK B INNER JOIN AUTHOR_BOOK AB ON " +
					"B.id=AB.bookId where AB.authorId=?");

		preparedStatement.setLong(
			1, Long.parseLong(authorContext.andMap(AuthorQuerier::id).get()));

		ResultSet resultSet = preparedStatement.executeQuery();

		return StreamSupport.stream(
			new BookContextSpliterator(this, resultSet), false);
	}

	static class BookContextFromQuerier implements BookContext {
		private final BookQuerier _querier;

		public BookContextFromQuerier(BookQuerier querier) {
			_querier = querier;
		}

		@Override
		public <R> Result<R> andMap(
			Function<BookQuerier, R> mapper) {

			return new CorrectResult<>(
				mapper.apply(_querier));
		}

		@Override
		public BookContext execute(SqlCommand<BookQuerier> command) {
			return this;
		}
	}

}
