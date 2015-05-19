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
import org.funbizmodel.bookstore.service.CorrectResult;
import org.funbizmodel.bookstore.service.ErrorResult;
import org.funbizmodel.bookstore.service.ReadOnlyContext;
import org.funbizmodel.bookstore.service.Result;
import org.funbizmodel.bookstore.service.SqlCommand;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

/**
* @author Carlos Sierra Andrés
*/
class BookCreationContext implements BookContext {

	private final Consumer<BookBuilder> _consumer;
	private BookBuilder _bookBuilder;
	private volatile long _createdId = -1;
	private SQLException _exception;
	private Connection _conn;

	public BookCreationContext(Connection conn, Consumer<BookBuilder> consumer) {
		_conn = conn;
		_consumer = consumer;
	}

	@Override
	public <R> Result<R> andMap(Function<BookQuerier, R> mapper) {
		try {
			if (_exception != null) {
				throw _exception;
			}
			long id = _doInsert();

			return new CorrectResult<>(
				mapper.apply(
					new BookQuerierFromBuilder(id, _bookBuilder)));

		}
		catch (SQLException e) {
			ErrorResult<R> errorResult = new ErrorResult<>();

			errorResult.addError(e.getMessage());

			return errorResult;
		}
	}

	private long _doInsert() throws SQLException {
		if (_createdId == -1) {
			_bookBuilder = new BookBuilder();

			_consumer.accept(_bookBuilder);

			//Merge validation errors

			String sql = createSQL();

			PreparedStatement preparedStatement =
				_conn.prepareStatement(
					sql, Statement.RETURN_GENERATED_KEYS);

			int affectedRows = preparedStatement.executeUpdate();

			if (affectedRows == 0) {
				throw new SQLException("Failed to execute " + sql);
			}

			try (ResultSet keysResult =
					 preparedStatement.getGeneratedKeys()) {

				keysResult.next();

				_createdId = keysResult.getLong(1);

				_bookBuilder._authorContexts.forEach(ac -> {
					Result<String> authorId = ac.andMap(AuthorQuerier::id);

					try {
						PreparedStatement authorAddStatement =
							_conn.prepareStatement(
							"INSERT INTO AUTHOR_BOOK (authorId, bookId) " +
								"values (?, ?)");
						authorAddStatement.setLong(
							1, Long.parseLong(authorId.get()));

						authorAddStatement.setLong(2, _createdId);

						authorAddStatement.executeUpdate();
					}

					catch (SQLException e) {
						e.printStackTrace();
					}

				});
			}

		}

		return _createdId;
	}

	private String createSQL() {
		return "insert into Book (isbn, title) " + "values ('" +
			_bookBuilder._isbn + "', '" + _bookBuilder._title + "')";
	}

	@Override
	public BookContext execute(SqlCommand<BookQuerier> command) {
		return this;
	}


	private class BookQuerierFromBuilder implements BookQuerier {
		private long _id;
		private BookBuilder _bookBuilder;

		public BookQuerierFromBuilder(long id, BookBuilder bookBuilder) {
			_id = id;
			_bookBuilder = bookBuilder;
		}

		@Override
		public long id() {
			return _id;
		}

		@Override
		public String isbn() {
			return _bookBuilder._isbn;
		}

		@Override
		public String title() {
			return _bookBuilder._title;
		}

		@Override
		public <R> Stream<R> authors(Function<AuthorQuerier, R> function) {
			return _bookBuilder._authorContexts.map(
				ac -> ac.andMap(function).get());
		}
	}
}
