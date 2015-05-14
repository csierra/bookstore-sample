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
import org.funbizmodel.bookstore.model.book.BookService;
import org.funbizmodel.bookstore.service.CorrectResult;
import org.funbizmodel.bookstore.service.ErrorResult;
import org.funbizmodel.bookstore.service.ReadOnlyContext;
import org.funbizmodel.bookstore.service.Result;
import org.funbizmodel.bookstore.service.SqlCommand;
import org.funbizmodel.bookstore.service.SqlCommandContext;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * @author Carlos Sierra Andrés
 */
public class AuthorService {
	private Connection _conn;

	public void setBookService(BookService bookService) {
		_bookService = bookService;
	}

	private BookService _bookService;

	public AuthorService(Connection conn) {
		_conn = conn;
	}

	public AuthorContext create(Consumer<AuthorBuilder> consumer) {

		return new AuthorCreationContext(consumer);
	}

	public Stream<AuthorContext> create(Consumer<AuthorBuilder> ... consumers) {
		return Stream.of(consumers).map(this::create);
	}

	public Stream<AuthorContext> create(
		Stream<Consumer<AuthorBuilder>> consumers) {

		return consumers.map(this::create);
	}

	public AuthorContext withId(String id) {
		return new OnlyAuthorContext(id);
	}

	public Stream<AuthorContext> all() {
		return Stream.<AuthorContext>builder().build();
	}

	private class AuthorCreationContext implements AuthorContext {
		private final Consumer<AuthorBuilder> _consumer;
		private AuthorBuilder _authorBuilder;
		private volatile long _createdId = -1;
		private SQLException _exception;

		public AuthorCreationContext(Consumer<AuthorBuilder> consumer) {
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
						new AuthorQuerierFromBuilder(id, _authorBuilder)));

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
				}

				Stream<BookContext> bookContextStream =
					_authorBuilder.books.apply(this);

				bookContextStream.forEach(bc -> bc.andMap(BookQuerier::id));
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

	private class OnlyAuthorContext implements AuthorContext {
		private String _id;

		public OnlyAuthorContext(String id) {
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
						preparedStatement = _conn.prepareStatement(query);

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
				_conn.prepareStatement(sql);

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
					return _bookService.fromAuthor(new OnlyAuthorContext(id()));
				}
				catch (SQLException e) {
					return Stream.empty();
				}
			}

		}
	}

	private class AuthorQuerierFromBuilder implements AuthorQuerier {

		private long _id;
		private AuthorBuilder _authorBuilder;

		public AuthorQuerierFromBuilder(
			long id, AuthorBuilder authorBuilder) {

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
			return _authorBuilder.books.apply(
				new OnlyAuthorContext(Long.toString(_id)));
		}
	}

	public static SqlCommand<AuthorQuerier> update(String newName) {
		return (cc) -> {
			//FIXME: little bobby tables
			cc.addSql("UPDATE AUTHOR SET NAME='" + newName+"'");
		};
	};
}
