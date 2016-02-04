package com.lzh.queue;

import java.util.LinkedList;
import java.util.Queue;

import com.lzh.task.InstructTask;

class QueueTask {

	public boolean mQuiting = false;
	private Queue<InstructTask<?, ?>> mQueue = null;

	protected QueueTask() {
		mQuiting = false;
		mQueue = new LinkedList<InstructTask<?, ?>>();
	}

	protected void put(InstructTask<?, ?> task) {
		if (task == null) {
			return;
		}
		synchronized (this) {
			if (mQuiting) {
				return;
			} 
			mQueue.add(task);
			notifyAll();
		}
	}

	protected InstructTask<?, ?> next() {
		synchronized (this) {
			if (mQueue.isEmpty()) {
				return null;
			}
			return mQueue.poll();
		}
	}

	protected void clear() {
		synchronized (this) {
			mQueue.clear();
		}
	}

	protected final void quit() {
		synchronized (this) {
			if (mQuiting) {
				return;
			}
			mQuiting = true;
			notifyAll();
		}
	}
}
