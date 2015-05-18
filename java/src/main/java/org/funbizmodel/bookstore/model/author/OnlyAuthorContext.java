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
import org.funbizmodel.bookstore.service.CorrectResult;
import org.funbizmodel.bookstore.service.ErrorResult;
import org.funbizmodel.bookstore.service.ReadOnlyContext;
import org.funbizmodel.bookstore.service.Result;
import org.funbizmodel.bookstore.service.SqlCommand;
import org.funbizmodel.bookstore.service.SqlCommandContext;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

/**
* @author Carlos Sierra Andrés
*/
class OnlyAuthorContext implements AuthorContext {
	private AuthorService _authorService;
	private String _id;

	public OnlyAuthorContext(AuthorService authorService, String id) {
		_authorService = authorService;
		_id = id;
	}

	@Override
	public <R> Result<R> andMap(Function<AuthorQuerier, R> mapper) {

		try {
			return new CorrectResult<R>(
				withResultSet(
					(resultSet) ->
						mapper.apply(
							new AuthorQuerierFromResult(resultSet))));
		}
		catch (SQLException e) {

			ErrorResult<R> errorResult = new ErrorResult<>();

			errorResult.addError(e.getMessage());

			return errorResult;
		}
	}

	@Override
	public void execute(SqlCommand<AuthorQuerier> command) {

		List<String> sqls = new ArrayList<>();

		command.accept(new SqlCommandContext<AuthorQuerier>() {
			@Override
			public void addSql(String sql) {
				sqls.add(sql);
			}

			@Override
			public AuthorQuerier get() {
				try {
					return withResultSet(AuthorQuerierFromResult::new);
				}
				catch (SQLException e) {
					//TODO: append errors to context
					throw new RuntimeException();
				}
			}
		});

		for (String sql : sqls) {
			String query = sql + " WHERE id = " + _id;

			try {
				PreparedStatement preparedStatement =
					preparedStatement = _authorService.conn.prepareStatement(query);

				preparedStatement.executeUpdate();
			}
			catch (SQLException e) {
				//TODO: append errors to context
				throw new RuntimeException(e);
			}
		}
	}

	private <T> T withResultSet(
			Function<ResultSet, T> mapping)
		throws SQLException {

		String sql = "select * from author where id=" + _id;

		PreparedStatement preparedStatement =
			_authorService.conn.prepareStatement(sql);

		ResultSet resultSet = preparedStatement.executeQuery();

		return mapping.apply(resultSet);

	}

	private class AuthorQuerierFromResult implements AuthorQuerier {
		private ResultSet _resultSet;

		public AuthorQuerierFromResult(ResultSet resultSet) {

			_resultSet = resultSet;

			try {
				_resultSet.next();
			}
			catch (SQLException e) {
				throw new RuntimeException(e);
			}
		}

		@Override
		public String id() {
			try {
				return Long.toString(_resultSet.getLong("id"));
			}
			catch (SQLException e) {
				throw new RuntimeException(e);
			}
		}

		@Override
		public String name() {
			try {
				return _resultSet.getString("name");
			}
			catch (SQLException e) {
				throw new RuntimeException(e);
			}
		}

		@Override
		public Stream<? extends ReadOnlyContext<BookQuerier>> books() {
			try {
				return _authorService.bookService.fromAuthor(new OnlyAuthorContext(_authorService, id()));
			}
			catch (SQLException e) {
				return Stream.empty();
			}
		}

	}
}
