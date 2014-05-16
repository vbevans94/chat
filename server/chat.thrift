namespace java thrift.entity

struct User
{
  1:  string username
  2:  string passhash
}

struct Message 
{
  1: string data
  2: User author
}

struct Dialog
{
  1: User partner
  2: Message lastMessage
}

enum ErrorType
{
  NO_SUCH_USER = 404,
  USER_ALREADY_EXISTS = 401,
  INVALID_DATA = 400
}

/**
 * Exception that can be raised by service.
 */
exception ChatException {
  1: ErrorType errorType,
  2: string message
}

/** Manages user interaction */
service Chat
{
  /**
   * Registers user in the system.
   * @throws ChatException when there were validation errors or such user already exists
   */
  void registerUser(1: User user) throws (1: ChatException error),

  /**
   * Retrieves all users currently registered in the system.
   * @return list of all users
   * @throws ChatException when there is no such user that tries to authenticate.
   */
  list<User> getAllUsers(1: User user) throws (1: ChatException error),
  
  /**
   * Retrieves all opened dialogs of current user.
   * @return list of dialogs for current user
   * @throws ChatException when there is no such user that tries to authenticate.
   */
  list<Dialog> getDialogs(1: User user) throws (1: ChatException error),

  /**
   * Retrieves all messages in the given dialog.
   * @return list of messages in given dialog
   * @throws ChatException when there is no such user that tries to authenticate.
   */
  list<Message> getMessages(1: User user, 2: Dialog dialog) throws (1: ChatException error),

  /**
   * Sends a message in the form of dialog topping where user and a message are set.
   * @return updated list of messages in this dialog
   * @throws ChatException when there is no such user that tries to authenticate.
   */
  list<Message> sendMessage(1: User user, 2: Dialog dialog) throws (1: ChatException error)
}