package org.jmallory.smtp;

import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.jmallory.smtp.MailSender.MailSenderListener;

public class SendMail implements MailSenderListener {

    protected int                mails;
    protected int                concurrent;

    protected String             host;
    protected int                port;

    protected String             from;

    protected String             password;

    protected String             to;

    protected String             title;
    protected Date               startDate;

    protected int                dateIntervalInMinutes;

    protected int                pauseIntervalInMilliseconds;

    protected int                succeed;
    protected int                done;

    protected MailSenderListener listener;

    protected ExecutorService    executor;

    public SendMail(int mails, int concurrent, String host, int port, String from, String password,
                    String to, String title, Date startDate, int dateIntervalInMinutes,
                    int pauseIntervalInMilliseconds, MailSenderListener listener) {

        if (mails <= 0 || concurrent <= 0 || host == null || port <= 0 || from == null
                || to == null || title == null || startDate == null || dateIntervalInMinutes < 0) {
            throw new IllegalArgumentException("invalid parameter");
        }

        this.mails = mails;
        this.concurrent = concurrent;
        this.host = host;
        this.port = port;
        this.from = from;
        this.password = password;
        this.to = to;
        this.title = title;
        this.startDate = startDate;
        this.dateIntervalInMinutes = dateIntervalInMinutes;
        this.pauseIntervalInMilliseconds = pauseIntervalInMilliseconds;
        this.listener = listener;
    }

    public void send(final boolean autoShutdown) {
        executor = Executors.newFixedThreadPool(concurrent);

        Thread taskScheduler = new Thread(new Runnable() {

            @Override
            public void run() {
                long time = startDate.getTime();

                int digits = 0, tmp = mails;
                while (tmp > 0) {
                    digits += 1;
                    tmp = tmp / 10;
                }

                long pause = pauseIntervalInMilliseconds / concurrent;
                if (pause < 0)
                    pause = 0;

                for (int i = 1; i <= mails; i++) {
                    // execute
                    executor.execute(new MailSender(host, port, from, password, to, paddingNumber(
                            i, digits) + " " + title, new Date(time), listener != null ? listener
                            : SendMail.this));

                    // change date 
                    time -= (dateIntervalInMinutes * 60 * 1000);

                    if (pause >= 0) {
                        try {
                            Thread.sleep(pause);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }

                if (autoShutdown) {
                    executor.shutdown(); // this will wait for all the task to be finished
                }
            }
        });
        taskScheduler.start();
    }

    public void stop() {
        if (executor != null) {
            executor.shutdownNow();
        }
    }

    private String paddingNumber(int number, int digits) {
        StringBuilder sb = new StringBuilder();

        while (number > 0) {
            sb.append(number % 10);
            number = number / 10;
        }

        for (int i = 0, length = digits - sb.length(); i < length; i++) {
            sb.append('0');
        }

        return sb.reverse().toString();
    }

    @Override
    public synchronized void done(Exception e) {
        if (e != null) {
            e.printStackTrace();
            done++;
        } else {
            succeed++;
            done++;
        }

        System.out.print(".");

        if (done >= mails) {
            System.out.println("\nSucceed/Sent: " + succeed + "/" + done);
        }
    }
}
