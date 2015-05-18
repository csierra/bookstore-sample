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
import org.funbizmodel.bookstore.model.book.BookQuerier;
import org.funbizmodel.bookstore.service.CorrectResult;
import org.funbizmodel.bookstore.service.ErrorResult;
import org.funbizmodel.bookstore.service.Result;
import org.funbizmodel.bookstore.service.SqlCommand;

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
class AuthorCreationContext implements AuthorContext {

	private AuthorService _authorService;
	private final Consumer<AuthorBuilder> _consumer;
	private AuthorBuilder _authorBuilder;
	private volatile long _createdId = -1;
	private SQLException _exception;

	public AuthorCreationContext(
		AuthorService authorService, Consumer<AuthorBuilder> consumer) {

		_authorService = authorService;
		_consumer = consumer;
	}

	@Override
	public <R> Result<R> andMap(Function<AuthorQuerier, R> mapper) {
		try {
			if (_exception != null) {
				throw _exception;
			}
			long id = doInsert();

			return new CorrectResult<>(
				mapper.apply(
					new AuthorQuerierFromBuilder(
						_authorService, id, _authorBuilder)));

		}
		catch (SQLException e) {
			ErrorResult<R> errorResult = new ErrorResult<>();

			errorResult.addError(e.getMessage());

			return errorResult;
		}
	}

	private long doInsert() throws SQLException {
		if (_createdId == -1) {
			_authorBuilder = new AuthorBuilder();

			_consumer.accept(_authorBuilder);

			String sql = createSQL(_authorBuilder);

			PreparedStatement preparedStatement =
				_authorService.conn.prepareStatement(
					sql, Statement.RETURN_GENERATED_KEYS);

			int affectedRows = preparedStatement.executeUpdate();

			if (affectedRows == 0) {
				throw new SQLException("Failed to execute " + sql);
			}

			try (ResultSet keysResult =
					 preparedStatement.getGeneratedKeys()) {

				keysResult.next();

				_createdId = keysResult.getLong(1);
			}

			Stream<Result<Long>> resultStream =
				_authorBuilder.books.map(bc -> bc.andMap(BookQuerier::id));

			resultStream.forEach(r -> {
				Long bookId = r.get();

				PreparedStatement addBookStatement =
					null;
				try {
					addBookStatement = _authorService.conn.prepareStatement(
						"INSERT INTO AUTHOR_BOOK (authorId, bookId) values " +
							"(?,?)");
					addBookStatement.setLong(1, _createdId);
					addBookStatement.setLong(2, bookId);

					addBookStatement.executeUpdate();
				}
				catch (SQLException e) {
					throw new RuntimeException(e);
				}

			});
		}

		return _createdId;
	}

	private String createSQL(AuthorBuilder authorBuilder) {
		return "insert into author(name) values('" +
			authorBuilder.name + "')";
	}

	@Override
	public void execute(SqlCommand<AuthorQuerier> command) {

	}

}
