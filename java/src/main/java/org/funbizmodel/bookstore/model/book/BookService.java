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

import org.funbizmodel.bookstore.Command;
import org.funbizmodel.bookstore.model.author.AuthorQuerier;
import org.funbizmodel.bookstore.model.author.AuthorService;
import org.funbizmodel.bookstore.service.CorrectResult;
import org.funbizmodel.bookstore.service.ErrorResult;
import org.funbizmodel.bookstore.service.ReadOnlyContext;
import org.funbizmodel.bookstore.service.Result;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Optional;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * @author Carlos Sierra Andrés
 */
public class BookService {

	private Connection _conn;
	private AuthorService _authorService;

	public void setAuthorService(AuthorService authorService) {
		_authorService = authorService;
	}

	public BookService(Connection conn) {
		_conn = conn;
	}

	public BookContext create(Consumer<BookBuilder> consumer) {
		return new BookCreationContext(consumer);
	}

	public Stream<BookContext> create(Consumer<BookBuilder> ... consumers) {
		return Stream.of(consumers).map(BookCreationContext::new);
	}

	public Stream<BookContext> create(Stream<Consumer<BookBuilder>> consumers) {
		return consumers.map(BookCreationContext::new);
	}

	public BookContext withId(String id) {
		return null;
	}

	public Stream<BookContext> fromTitles(String ... titles)
		throws SQLException {

		PreparedStatement preparedStatement =
			_conn.prepareStatement(
				"select * from TABLE(X varchar=?) T inner join BOOK on " +
					"T.x=Book.title");

		preparedStatement.setObject(1, (String[])titles);

		ResultSet resultSet = preparedStatement.executeQuery();

		return StreamSupport.stream(
			new BookContextSpliterator(resultSet), false);
	}

	public Stream<BookContext> fromAuthor(
		ReadOnlyContext<AuthorQuerier> authorContext) throws SQLException {

		PreparedStatement preparedStatement =
			_conn.prepareStatement("select * from BOOK where authorId=?");

		preparedStatement.setLong(
			1, Long.parseLong(authorContext.andMap(AuthorQuerier::id).get()));

		ResultSet resultSet = preparedStatement.executeQuery();

		return StreamSupport.stream(
			new BookContextSpliterator(resultSet), false);
	}

	private static class BookContextFromQuerier implements BookContext {
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
		public void execute(Command<BookQuerier> command) {
			command.accept(() -> _querier);
		}
	}

	private class BookContextSpliterator
		implements Spliterator<BookContext> {

		private final ResultSet _resultSet;

		public BookContextSpliterator(ResultSet resultSet) {
			_resultSet = resultSet;
		}

		@Override
		public boolean tryAdvance(Consumer<? super BookContext> action) {
			Optional<BookQuerier> maybeQuerier =
				BookQuerier.fromResultSet(_authorService, _resultSet);

			maybeQuerier.ifPresent(
				querier -> action.accept(
					new BookContextFromQuerier(querier)));

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

	private class BookCreationContext implements BookContext {

		private final Consumer<BookBuilder> _consumer;
		private BookBuilder _bookBuilder;
		private volatile long _createdId = -1;
		private SQLException _exception;

		public BookCreationContext(Consumer<BookBuilder> consumer) {
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

				Result<String> authorIdResult =
					_bookBuilder._authorContext.andMap(
						AuthorQuerier::id);

				//Merge validation errors

				String sql = createSQL(authorIdResult);

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
			}

			return _createdId;
		}

		private String createSQL(Result<String> authorIdResult) {
			return "insert into Book (isbn, title, authorId) " +
				"values ('" + _bookBuilder._isbn + "', '" +
				_bookBuilder._title + "', '"+ authorIdResult.get() +"')";
		}

		@Override
		public void execute(Command<BookQuerier> command) {

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
			public ReadOnlyContext<AuthorQuerier> author() {
				System.out.println(
					"select * from Author,Book where author.id = " +
						"book.authorId and book.id = " + id());

				return _authorService.withId("id");
			}
		}
	}
}
