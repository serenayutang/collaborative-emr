package client;

import model.*;
import server.*;
import doctor_database.Doctor;

import java.util.ArrayList;
import java.util.List;

/**
 * A thread receives notifications from server every second.
 */
public class CheckMessage implements Runnable {

  private List<String> messageList;
  private boolean isAlive = true;
  private ServerInterface serverInterface;
  private Doctor doctor;

  public CheckMessage(ServerInterface serverInterface) {
    this.serverInterface = serverInterface;
  }

  /**
   * Fetch notifications from server every second
   */
  @Override
  public void run() {
    while (isAlive) {
      try {
        if (doctor != null) {
          Result result = serverInterface.getNotifications(doctor);
          List<String> unReadMessages = result.getUnreadNotifications();
          if (null != unReadMessages && !unReadMessages.isEmpty()) {
            System.out.println("You have a new notification.");
            messageList = unReadMessages;
            doctor.setNotifications(unReadMessages);
            stop();
          }
        }
        Thread.sleep(1000);
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  void stop() {
    isAlive = false;
  }

  /**
   * Get all new notifications.
   */
  List<String> getAllMessages() {
    List<String> res = new ArrayList<>();
    if (null != messageList) {
      synchronized (messageList) {
        if (!messageList.isEmpty()) {
          res.addAll(messageList);
          messageList.clear();
        }
      }
    }
    return res;
  }

  /**
   * Clear notifications.
   */
  void clearMessageList() {
    if (null != messageList) {
      synchronized (messageList) {
        messageList.clear();
      }
    }
  }

  public Doctor getDoctor() {
    return this.doctor;
  }

  public void setDoctor(Doctor doctor) {
    this.doctor = doctor;
  }
}
