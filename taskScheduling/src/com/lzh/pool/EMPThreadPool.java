package com.lzh.pool;

import java.util.Vector;

import android.os.Looper;


/**
 * 线程�?
 */
public class EMPThreadPool {
    private static final int NUM_THREADS = 3;
    public static int PRIORITY_OFFSTORE = 200000;//指定离线存储优先�?

    private int mRunning;

    /** 存储与前台相关的任务 */
    private final Vector<Runnable> mQueue = new Vector<Runnable>(2);	

    /**
     * <p>
     * 线程池构造方�?
     * </p>
     */
    public EMPThreadPool() {
        mRunning = 0;
        initWorkQueue();
    }

    /**
     * 构�?�Work
     * 两个线程负责�?测前台任务队列，�?个线程负责检测后台任务队�?
     */
    private void initWorkQueue() {
    	new Worker(this, 0, 0).start(); // This worker only for task.priority = 0.
    	new Worker(this, 0, 100000).start();
    	
    	new Worker(this, PRIORITY_OFFSTORE, PRIORITY_OFFSTORE).start();
    }

    /**
     * <p>
     * Adds a foreground task who's run() method will be invoked asynchronously
     * by a worker thread.
     * </p>
     * 
     * @param task a Runnable instance
     */
    public void execute(Runnable task) {
		if (task == null) {
			return;
		}
		synchronized (mQueue) {
			boolean flag = true;
			
			if (task instanceof Task) {
				for (int i = mQueue.size() - 1; i >= 0; i--) {
					Task temp = (Task) mQueue.elementAt(i);
					if (((Task) task).mPriority >= temp.mPriority) {
						mQueue.insertElementAt(task, i + 1);
						flag = false;
						break;
					}
				}
				if (flag) {
					mQueue.insertElementAt(task, 0);
				}
			}
			mQueue.notifyAll();
		}
    }
    
    public int getRunning() {
        return mRunning;
    }
    
    public void removeTask(Runnable task) {
        synchronized (mQueue) {
            mQueue.removeElement(task);
        }
    }
    
    public void onDestroy() {
    	
    }

    /**
     * 
     */
    public abstract static class Task implements Runnable {

        private String mName;
        private Throwable mThrowable = null;
        private String mErrorMessage = null;
        private int mPriority;
        private boolean mStop;

        /**
         * <p>
         * 任务类的构�?�方法�??
         * </p>
         * 
         * @param priority 优先级�??
         */
        public Task(int priority) {
            mName = null;
            mPriority = priority;
            mStop = false;
        }

        /**
         * <p>
         * 任务类的构�?�方法�??
         * </p>
         * 
         * @param name 任务名称
         * @param priority 优先�?
         */
        public Task(String name, int priority) {
            mName = name;
            mPriority = priority;
            mStop = false;
        }

        public String getName() {
            return mName;
        }
        
        /**
         * <p>
         * 获得异常对象
         * </p>
         * 
         * @return 异常对象�?
         */
        public Throwable getThrowable() {
            return mThrowable;
        }

        /**
         * <p>
         * 设置异常对象�?
         * </p>
         * 
         * @param e 异常对象
         */
        void setThrowable(Throwable e) {
            mThrowable = e;
        }

        /**
         * <p>
         * 获得异常信息�?
         * </p>
         * 
         * @return 异常信息�?
         */
        public String getErrorMessage() {
            if (mErrorMessage == null && mThrowable != null) {
                return mThrowable.toString();
            }
            else {
                return mErrorMessage;
            }
        }

        /**
         * <p>
         * 设置异常信息�?
         * </p>
         * 
         * @param message 异常信息
         */
        public void setErrorMessage(String message) {
            mErrorMessage = message;
        }

        /**
         * <p>
         * 从异常对象中获得异常信息，设置到异常信息变量中�??
         * </p>
         * 
         * @param e 异常对象�?
         */
        public void setErrorMessage(Throwable e) {
            mErrorMessage = (e == null) ? null : e.getMessage();
        }

        /**
         * <p>
         * 获得任务是否停止情况�?
         * </p>
         * 
         * @return 任务是否停止�?
         */
        public boolean isStop() {
            return mStop;
        }

        /**
         * <p>
         * 设置任务是否停止�?
         * </p>
         * 
         * @param stop 任务是否停止
         */
        public void setStop(boolean stop) {
            mStop = stop;
        }

        /**
         * <p>
         * 判断是否发生了异常�??
         * </p>
         * 
         * @return 是否发生了异常�??
         */
        public boolean hasFailed() {
            return (mThrowable != null || mErrorMessage != null);
        }

        /**
         * <p>
         * This is the main entrance point so that if any exception leaks out of
         * doRun(), we'll capture it here and set error msg accordingly.
         * </p>
         */
        @Override
        public void run() {
            try {
                doRun();
            }
            catch (Throwable ex) {
                setErrorMessage(ex.getMessage());
                setThrowable(ex);
            }
            // 重启线程处理后续操作
            new Thread() {
                public void run() {
                    if (Looper.myLooper() == null) {
                        Looper.prepare();
                    }
                    done();
                }
            }.start();
        }

        /**
         * <p>
         * Called when task finishes.
         * </p>
         */
        private void done() {
            if (isStop()) {
                return;
            }
            if (hasFailed()) {
                onFailure();
            }
            else {
                onSuccess();
            }
        }

        /**
         * <p>
         * We do not use Runnable::run() so that we can throw exceptions. We had
         * to throw the very basic exception because we don't know what are out
         * there.
         * </p>
         */
        abstract public void doRun() throws Exception;

        /**
         * <p>
         * 任务执行成功的回调函数�??
         * </p>
         */
        public void onSuccess() {
        }

        /**
         * <p>
         * 任务执行失败的回调函数�??
         * </p>
         */
        public void onFailure() {
        }
    }

    /**
     * 
     */
    private static class Worker extends Thread {
        
        protected final EMPThreadPool mPool;
        protected int mMinPriority;//�?小优先级 
        protected int mMaxPriority;//�?大优先级
        
        Worker(EMPThreadPool pool, int minPriority, int maxPriority) {
            super();
            mPool = pool;
            mMinPriority = minPriority;
            mMaxPriority = maxPriority;
        }

        public void run() {
            for (;;) {
            	Runnable task = null;
            	synchronized (mPool.mQueue) {
					try {
						if (mPool.mQueue.isEmpty()) {
							mPool.mQueue.wait();
						}
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
            		if(mPool.mQueue.isEmpty()){
            			continue;
            		}
            		Runnable taskTemp = null;
            		for(int i = 0 ; i < mPool.mQueue.size(); i++){
            			taskTemp = mPool.mQueue.elementAt(i);
            			if(((Task)taskTemp).mPriority >= mMinPriority 
            					&& ((Task)taskTemp).mPriority <= mMaxPriority){
            				task = taskTemp;
            				mPool.mQueue.removeElementAt(i);
            				System.out.print("ThreadPool"+"remove Task : " + ((Task) task).mPriority + "==" + this);
            				break;
            			} 
                	}
            	}
            	if(task != null){
            		synchronized (mPool) {
                        mPool.mRunning++;
                    }
					try {
						task.run();
					} catch (Exception e) {
						new Worker(mPool, mMinPriority, mMaxPriority).start();
						break;
					} finally {
						synchronized (mPool) {
							mPool.mRunning--;
						}
					}
            	}
            }
        }
    }
}
