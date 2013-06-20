/*
 * Copyright (C) 2013 Marten Gajda <marten@dmfs.org>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 */

package org.dmfs.rfc5545.recur;

import java.util.Collections;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;


/**
 * An abstract by-part filter. Depending on a parameter it expands or limits the instances returned by the previous filter.
 * 
 * @author Marten Gajda <marten@dmfs.org>
 */
abstract class ByFilter extends RuleIterator
{

	/**
	 * Stop iterating (throwing an exception) if this number of empty sets passed in a line, i.e. sets that contain no elements because they have been filtered
	 * or nothing was expanded.
	 */
	private final static int MAX_EMPTY_SETS = 1000;

	/**
	 * Stop iterating (throwing an exception) if this number of instances have been filtered in a line.
	 */
	private final static int MAX_EMPTY_INSTANCES = 1000;

	/**
	 * The scope of a part. Depending on the frequency and preceding filters some filters operate within a specific scope.
	 */
	protected enum Scope
	{
		WEEKLY, MONTHLY, WEEKLY_AND_MONTHLY, YEARLY;
	}

	/**
	 * A flag indicating if this filter is in expand mode.
	 */
	private final boolean mExpand;

	/**
	 * The current iterator. This is used by {@link #next()} if this filter is expanding.
	 */
	private Iterator<Instance> mCurrentIterator;

	/**
	 * The first instance to iterate.
	 */
	private final Instance mStart;

	/**
	 * The set we work on.
	 */
	private final TreeSet<Instance> mWorkingSet = new TreeSet<Instance>();

	/**
	 * The set we return to subsequent filters.
	 */
	private final Set<Instance> mResultSet = Collections.unmodifiableSet(mWorkingSet);


	/**
	 * Create a new filter that filters the instances returned by the previous {@link RuleIterator}. The parameter <code>expand</code> determines whether the
	 * filter should limit or expand the instances.
	 * 
	 * @param previous
	 *            The preceding {@link RuleIterator}.
	 * @param start
	 *            The first instance.
	 * @param expand
	 *            <code>true</code> to epxand the instances, <code>false</code> to limit them.
	 */
	public ByFilter(RuleIterator previous, Calendar start, boolean expand)
	{
		super(previous);
		mStart = new Instance(start);
		mExpand = expand;
	}


	@Override
	public Instance next()
	{
		Instance next;
		if (mExpand)
		{
			if (mCurrentIterator == null || !mCurrentIterator.hasNext())
			{
				nextSet();
				mCurrentIterator = mWorkingSet.iterator();
			}
			next = mCurrentIterator.next();
		}
		else
		{
			int counter = 0;
			do
			{
				if (counter == MAX_EMPTY_INSTANCES)
				{
					throw new IllegalArgumentException("too many filtered recurrence instances");
				}
				counter++;
				next = mPrevious.next();
			} while (filter(next));
		}
		return next;
	}


	@Override
	Set<Instance> nextSet()
	{
		mWorkingSet.clear();
		if (mExpand)
		{
			int counter = 0;
			do
			{
				for (Instance instance : mPrevious.nextSet())
				{
					if (counter == MAX_EMPTY_SETS)
					{
						throw new IllegalArgumentException("too many empty recurrence sets");
					}
					counter++;

					expand(mWorkingSet, instance, mStart);
				}
			} while (mWorkingSet.size() == 0);
		}
		else
		{
			int counter = 0;
			do
			{
				if (counter == MAX_EMPTY_SETS)
				{
					throw new IllegalArgumentException("too many empty recurrence sets");
				}
				counter++;
				for (Instance d : mPrevious.nextSet())
				{
					if (!filter(d))
					{
						mWorkingSet.add(d);
					}
				}
			} while (mWorkingSet.size() == 0);
		}
		return mResultSet;
	}


	/**
	 * Filter an instance. This method determines if a given {@link Instance} should be removed from the result set or not.
	 * 
	 * @param instance
	 *            The instance to filter.
	 * @return <code>true</code> to remove the instance from the result set, <code>false</code> to include it.
	 */
	abstract boolean filter(Instance instance);


	/**
	 * Expand an instance. This method expands an {@link Instance} into the given {@link TreeSet}.
	 * 
	 * @param instances
	 *            The {@link TreeSet} that gets the results.
	 * @param instance
	 *            The instance to expand.
	 * @param start
	 *            The first instance of the rule. An implementing filter can use this to avoid iterating instances that precede the first instance if it's save
	 *            to do so.
	 */
	abstract void expand(TreeSet<Instance> instances, Instance instance, Instance start);
}
