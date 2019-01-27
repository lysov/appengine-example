package com.tutorlift.appengine.user;

import com.google.api.server.spi.auth.EspAuthenticator;
import com.google.api.server.spi.config.*;
import com.google.api.server.spi.config.ApiMethod.HttpMethod;
import com.google.api.server.spi.response.*;
import com.google.appengine.api.datastore.*;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.appengine.api.datastore.Query.FilterPredicate;
import com.google.appengine.api.datastore.Query.SortDirection;
import com.google.appengine.api.utils.SystemProperty;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;

import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.UserRecord;
import com.google.firebase.auth.UserRecord.CreateRequest;
import com.google.firebase.auth.UserRecord.UpdateRequest;
import com.google.maps.GaeRequestHandler;
import com.google.maps.GeoApiContext;
import com.google.maps.GeocodingApi;
import com.google.maps.errors.ApiException;
import com.google.maps.model.GeocodingResult;
import com.stripe.Stripe;
import com.stripe.exception.*;
import com.stripe.model.Card;
import com.stripe.model.Customer;
import com.tutorlift.appengine.user.PaymentMethod.PaymentMethodConstant;
import com.tutorlift.appengine.user.Student.Property;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Contains the configuration details of the Tutorlift API.
 */
@Api(
    title = "Tutorlift API",
    name = "api",
    canonicalName = "tutorlift",
    version = "v1",
    namespace = @ApiNamespace(ownerDomain = "tutorlift.com", ownerName = "tutorlift.com"),
    authenticators = {EspAuthenticator.class},
    issuers = {
        @ApiIssuer(
            name = "firebase",
            issuer = "https://securetoken.google.com/tutorlift",
            jwksUri = "https://www.googleapis.com/service_accounts" +
                    "/v1/metadata/x509/securetoken@system.gserviceaccount.com"
        )
    },
    issuerAudiences = {
        @ApiIssuerAudience(name = "firebase", audiences = "tutorlift")
    }
)

final public class UserController {

  /* Logger */
  private static final Logger logger = Logger.getLogger(UserController.class.getName());

  /* Indicates if the server runs in productions */
  private Boolean isProduction;

  /**
   * Constants of exceptions.
   */
  private final static class Exception {
    static final String STUDENT_PROFILE_REQUIRED = "Student Profile Required to be Created First";
    static final String EMAIL_REQUIRED = "Email Required";
    static final String EMAIL_CONFIRMATION_REQUIRED = "Email Confirmation Required";
    static final String DISABLED_ACCOUNT = "Account is disabled";
    static final String INVALID_EMAIL = "Invalid Email";
    static final String FIRST_NAME_REQUIRED = "First Name Required";
    static final String INVALID_FIRST_NAME = "Invalid First Name";
    static final String LAST_NAME_REQUIRED = "Last Name Required";
    static final String INVALID_LAST_NAME = "Invalid Last Name";
    static final String INVALID_HEADLINE = "Invalid Headline";
    static final String INVALID_BIO = "Invalid Bio";
    static final String POSTAL_CODE_REQUIRED = "Postal Code Required";
    static final String INVALID_POSTAL_CODE = "Invalid Postal Code";
    static final String RATE_REQUIRED = "Rate Required";
    static final String INVALID_RATE = "Invalid Rate";
    static final String LESSON_TYPE_REQUIRED = "Lesson Type Required";
    static final String INVALID_COURSE = "Invalid Course";
    static final String COURSE_REQUIRED = "At Least 1 Course Required";
    static final String INVALID_PAYMENT_METHOD = "Invalid payment method";
    /* 400 Error */
    static final String BAD_REQUEST = "Bad Request";
    /* 401 Error */
    static final String UNAUTHORIZED = "Unauthorized";
    /* 404 Error */
    static final String NOT_FOUND = "Resource Not Found";
    /* 5XX Error */
    static final String INTERNAL = "Internal Server Error";
    /* Query Parameters */
    static final String INVALID_TUTOR_PROPERTY_IN_THE_QUERY = "Invalid Tutor Property In The Query";
    static final String INVALID_COURSE_IN_THE_QUERY = "Invalid Course In The Query";
    static final String COURSE_REQUIRED_IN_THE_QUERY = "Course Required In The Query";
    static final String INVALID_PAGE_IN_THE_QUERY = "Invalid Page In The Query";
    static final String INVALID_PER_PAGE_IN_THE_QUERY = "Invalid Per Page In The Query";
  }

  /**
   * Constants of method names.
   */
  private final static class MethodName {
    /* Student */
    static final String GET_STUDENTS = "students.list";
    static final String POST_STUDENTS = "students.insert";
    static final String PUT_STUDENTS = "students.update";
    /* Tutor */
    static final String GET_TUTORS = "tutors.list";
    static final String POST_TUTORS = "tutors.insert";
    static final String PUT_TUTORS = "tutors.update";
    /* Search */
    static final String GET_SEARCH = "search.list";
    /* Courses */
    static final String GET_COURSES = "courses.list";
    /* Payment */
    static final String GET_PAYMENT = "payment.list";
    static final String POST_PAYMENT = "payment.insert";
    static final String DELETE_PAYMENT = "payment.delete";
  }

  /**
   * Constants of path names.
   */
  private final static class PathName {
    /* Student */
    static final String GET_STUDENTS = "students/{id}";
    static final String POST_STUDENTS = "students";
    static final String PUT_STUDENTS = "students";
    /* Tutor */
    static final String GET_TUTORS = "tutors/{id}";
    static final String POST_TUTORS = "tutors";
    static final String PUT_TUTORS = "tutors";
    /* Search */
    static final String GET_SEARCH = "search";
    /* Courses */
    static final String GET_COURSES = "courses";
    /* Payment */
    static final String GET_PAYMENT = "payment";
    static final String POST_PAYMENT = "payment";
    static final String DELETE_PAYMENT = "payment";
  }

  /** Constants of user types. */
  private final static class UserType {
    static final String STUDENT = "Student";
    static final String TUTOR = "Tutor";
  }

  private final static class SEARCH_QUERY_PROPERTY {
    static final String ID = "id";
    static final String PROPERTIES = "properties";
    /* online or in-class */
    static final String LESSON_TYPE = "lesson-type";
    static final String COURSE = "courses";
    static final String CITY = "city";
    static final String DEFAULT_CITY = "Calgary";
    static final String PROVINCE = "province";
    static final String DEFAULT_PROVINCE = "Alberta";
    static final String PER_PAGE = "per-page";
    static final String SEARCH_PER_PAGE_DEFAULT_VALUE = "20";
    static final String PAGE = "page";
    static final String SUBJECTS_PER_PAGE_DEFAULT_VALUE = "50";
  }

  /** Other constants. */
  private final static class Constant {
    static final String TRUE = "TRUE";
    static final String IS_DEBUG = "IS_DEBUG";
    static final String STRIPE_API_KEY = "STRIPE_API_KEY";
    static final String STRIPE_TEST_API_KEY = "STRIPE_TEST_API_KEY";
    static final String GOOGLE_GEOCODING_API_KEY = "GOOGLE_GEOCODING_API_KEY";
    static final String FIREBASE_SERVICE_ACCOUNT_KEY_PATH = "FIREBASE_SERVICE_ACCOUNT_KEY_PATH";
    static final String FIREBASE_DATABASE_URL = "FIREBASE_DATABASE_URL";
  }

  /**
   * Constructor for UserController.
   */
  public UserController() {
    /* Set up Firebase */
    isProduction = SystemProperty.environment.value() == SystemProperty.Environment.Value.Production;
    FirebaseOptions options;
    try {
      if (isProduction) {
        options = new FirebaseOptions.Builder()
            .setCredentials(GoogleCredentials.getApplicationDefault())
            .setDatabaseUrl(System.getenv(Constant.FIREBASE_DATABASE_URL))
            .build();
      } else {
        FileInputStream serviceAccount = new FileInputStream(
            new File(System.getenv(Constant.FIREBASE_SERVICE_ACCOUNT_KEY_PATH)));
        options = new FirebaseOptions.Builder()
            .setCredentials(GoogleCredentials.fromStream(serviceAccount))
            .setDatabaseUrl(System.getenv(Constant.FIREBASE_DATABASE_URL))
            .build();
      }
      FirebaseApp.initializeApp(options);
    } catch (IOException e) {
      logger.log(Level.SEVERE, Exception.INTERNAL);
      e.printStackTrace();
    }

    /* Set up Stripe API key*/
    String isDebug = System.getenv(Constant.IS_DEBUG);
    if (isDebug.equals(Constant.TRUE)) {
      Stripe.apiKey = System.getenv(Constant.STRIPE_TEST_API_KEY);
    } else {
      Stripe.apiKey = System.getenv(Constant.STRIPE_API_KEY);
    }
  }

  /**
   * Returns a student profile.
   * @param user Firebase user object.
   * @param id Unique firebase ID associated with a user.
   * @return The requested student object.
   * @throws UnauthorizedException
   * @throws com.google.api.server.spi.response.NotFoundException
   * @throws InternalServerErrorException
   */
  @ApiMethod(
      name = MethodName.GET_STUDENTS,
      path = PathName.GET_STUDENTS,
      httpMethod = HttpMethod.GET)
  public Student getStudent(final com.google.appengine.api.users.User user,
      @Named(Student.Property.ID) String id)
      throws UnauthorizedException,
      com.google.api.server.spi.response.NotFoundException, InternalServerErrorException {

    authorize(user);

    /* Construct a student entity key */
    Key studentKey = generateStudentKey(id);
    /* Fetch a must existing student entity from the database */
    Entity studentEntity = getExistingEntity(studentKey);

    return convertStudentEntityToStudent(studentEntity);
  }

  /**
   * Set custom jwt token fields that are not included in Firebase. Ex: isAdmin : true
   * @param user Firebase user object.
   * @param claims map containing custom jwt tokens.
   * @throws InternalServerErrorException
   */
  private void setCustomUserClaims(com.google.appengine.api.users.User user,
      Map<String, Object> claims)
      throws InternalServerErrorException {
    try {
      FirebaseAuth.getInstance().setCustomUserClaims(user.getUserId(), claims);
    } catch (FirebaseAuthException e) {
      e.printStackTrace();
      logger.log(Level.SEVERE, Exception.INTERNAL);
      throw new InternalServerErrorException(Exception.INTERNAL);
    }
  }

  /**
   * Add new student entity to database.
   * @param user Firebase user object.
   * @param student student object.
   * @return student object
   * @throws BadRequestException
   * @throws UnauthorizedException
   * @throws InternalServerErrorException
   */
  @ApiMethod(
      name = MethodName.POST_STUDENTS,
      path = PathName.POST_STUDENTS,
      httpMethod = HttpMethod.POST)
  public Student postStudent(final com.google.appengine.api.users.User user,
      Student student) throws BadRequestException, UnauthorizedException,
      InternalServerErrorException {

    authorize(user);

    validate(student);

    /* Set admin privilege on the user corresponding to uid. */
    Map<String, Object> claims = new HashMap<>();
    claims.put("isAdmin", false);
    setCustomUserClaims(user, claims);
    /* The new custom claims will propagate to the user's ID token the
    next time a new one is issued.
     */

    /* Set the properties that are not allowed to be set by a user */
    student.setId(user.getUserId());
    student.setEmail(user.getEmail());
    student.setDefaultPaymentMethod(PaymentMethodConstant.CASH);
    student.setUserType(UserType.STUDENT);

    /* Construct a student entity key */
    Key studentKey = generateStudentKey(student.getId());
    /*
    There must be neither user nor student entities with the id equal
      to the id property of the :User
     */
    validateEntityNonexistence(studentKey);

    /* Create a new User entity */
    Entity userEntity = new Entity(User.Property.KIND, student.getId());

    /* Create a new Student entity */
    Entity studentEntity = new Entity(Student.Property.KIND, student.getId(), userEntity.getKey());
    /* Assign properties to new student entity */
    studentEntity.setUnindexedProperty(Student.Property.ID, student.getId());
    studentEntity.setUnindexedProperty(Student.Property.PICTURE_URL, student.getPictureURL());
    studentEntity.setUnindexedProperty(Student.Property.EMAIL, student.getEmail());
    studentEntity.setUnindexedProperty(Student.Property.FIRST_NAME, student.getFirstName());
    studentEntity.setUnindexedProperty(Student.Property.LAST_NAME, student.getLastName());
    studentEntity.setUnindexedProperty(Student.Property.HEADLINE, student.getHeadline());
    studentEntity.setUnindexedProperty(Student.Property.DEFAULT_PAYMENT_METHOD, student.getDefaultPaymentMethod());
    studentEntity.setUnindexedProperty(Student.Property.USER_TYPE, student.getUserType());

    /* Create Stripe Customer */
    Map<String, Object> customerParams = new HashMap<>();
    customerParams.put("email", student.getEmail());
    customerParams.put("description", "Customer for " + student.getId() + ".");

    try {
      Customer customer = Customer.create(customerParams);
      studentEntity.setIndexedProperty(
          Property.STRIPE_ID, customer.getId());
    } catch (
        AuthenticationException |
        InvalidRequestException |
        APIConnectionException |
        CardException |
        APIException e) {
      e.printStackTrace();
      throw new InternalServerErrorException(Exception.INTERNAL);
    }
    /* Creates a list of entities and passes that list of entities to batchPut */
    batchPut(Arrays.asList(userEntity, studentEntity));

    return student;
  }

  /**
   * Updates a student entity in the database.
   * @param user Firebase user object.
   * @param student Student object.
   * @return Updated student object.
   * @throws BadRequestException
   * @throws UnauthorizedException
   * @throws com.google.api.server.spi.response.NotFoundException
   * @throws InternalServerErrorException
   */
  @ApiMethod(
      name = MethodName.PUT_STUDENTS,
      path = PathName.PUT_STUDENTS,
      httpMethod = ApiMethod.HttpMethod.PUT)
  public Student putStudent(final com.google.appengine.api.users.User user, Student student)
      throws BadRequestException, UnauthorizedException,
      com.google.api.server.spi.response.NotFoundException, InternalServerErrorException {

    authorize(user);

    validate(student);

    /* Construct a student entity key */
    Key studentKey = generateStudentKey(user.getUserId());

    /* Fetch a previously existing student entity from the database */
    Entity studentEntity = getExistingEntity(studentKey);

    /* Check if the user also has a tutor user */
    Boolean isTutor = (studentEntity.getProperty(Property.USER_TYPE)).equals(UserType.TUTOR);

    /* Flags that decide whether a tutor user will have to be updated */
    Boolean isPictureURLUpdated = false;
    Boolean isEmailUpdated = false;
    Boolean isFirstNameUpdated = false;
    Boolean isLastNameUpdated = false;
    Boolean isHeadlineUpdated = false;
    /* Update the entity properties */
    if (student.getPictureURL() != null) {
      studentEntity.setUnindexedProperty(Property.PICTURE_URL, student.getPictureURL());
      isPictureURLUpdated = true;
    }
    if (student.getEmail() != null) {
      studentEntity.setUnindexedProperty(Student.Property.EMAIL, student.getEmail());
      isEmailUpdated = true;
      /* Update Firebase Auth account */
      //Look into how to send verification email after user updates email.
      UpdateRequest request = new UpdateRequest(user.getUserId()).setEmail(student.getEmail()).setEmailVerified(false);
      try {
        FirebaseAuth.getInstance().updateUser(request);
      } catch (FirebaseAuthException e) {
        e.printStackTrace();
        throw new BadRequestException(Exception.BAD_REQUEST);
      }
    }
    if (student.getFirstName() != null) {
      studentEntity.setUnindexedProperty(Student.Property.FIRST_NAME, student.getFirstName());
      isFirstNameUpdated = true;
    }
    if (student.getLastName() != null) {
      studentEntity.setUnindexedProperty(Student.Property.LAST_NAME, student.getLastName());
      isLastNameUpdated = true;
    }
    if (student.getHeadline() != null) {
      studentEntity.setUnindexedProperty(Student.Property.HEADLINE, student.getHeadline());
      isHeadlineUpdated = true;
    }

    /* Update the default method of payment */
    if (student.getDefaultPaymentMethod() != null) {
      studentEntity.setUnindexedProperty(Property.DEFAULT_PAYMENT_METHOD, student.getDefaultPaymentMethod());
    }

    if (isTutor &&
        (isPictureURLUpdated ||
        isEmailUpdated ||
        isFirstNameUpdated ||
        isLastNameUpdated ||
        isHeadlineUpdated)) {

      /* Construct a tutor entity key */
      Key tutorKey = generateTutorKey(user.getUserId());

      /* Fetch a previously existing tutor entity from the database */
      Entity tutorEntity = getExistingEntity(tutorKey);

      if (isPictureURLUpdated) {
        tutorEntity.setUnindexedProperty(Tutor.Property.PICTURE_URL, student.getPictureURL());
      }
      if (isEmailUpdated) {
        tutorEntity.setUnindexedProperty(Tutor.Property.EMAIL, student.getEmail());
      }
      if (isFirstNameUpdated) {
        tutorEntity.setUnindexedProperty(Tutor.Property.FIRST_NAME, student.getFirstName());
      }
      if (isLastNameUpdated) {
        tutorEntity.setUnindexedProperty(Tutor.Property.LAST_NAME, student.getLastName());
      }
      if (isHeadlineUpdated) {
        tutorEntity.setUnindexedProperty(Tutor.Property.HEADLINE, student.getLastName());
      }

      batchPut(Arrays.asList(studentEntity, tutorEntity));

    } else {
      /*
        Properties that are shared between student and tutor profiles have not been updated,
        thus only the student user will be updated
       */
      putEntity(studentEntity);
    }

    return convertStudentEntityToStudent(studentEntity);
  }

  /**
   * Put Student or Tutor entity into database.
   * @param studentEntity Entity representing Student object.
   * @throws InternalServerErrorException
   */
  private void putEntity(Entity studentEntity) throws InternalServerErrorException {
    try {
      DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
      datastore.put(studentEntity);
    } catch (java.lang.Exception exception) {
      throw new InternalServerErrorException(Exception.INTERNAL);
    }
  }

  /**
   * Returns a Tutor.
   * @param user Firebase user object.
   * @param id Unique firebase ID associated with a user.
   * @return The requested Tutor object.
   * @throws UnauthorizedException
   * @throws NotFoundException
   * @throws InternalServerErrorException
   */
  @ApiMethod(
      name = MethodName.GET_TUTORS,
      path = PathName.GET_TUTORS,
      httpMethod = HttpMethod.GET)
  public Tutor getTutor(final com.google.appengine.api.users.User user,
      @Named(Tutor.Property.ID) String id)
      throws UnauthorizedException, NotFoundException, InternalServerErrorException {

    authorize(user);

    /* Construct a tutor entity key */
    Key tutorKey = generateTutorKey(id);

    /* Fetch a must existing tutor entity from the database */
    Entity tutorEntity = getExistingEntity(tutorKey);

    return convertTutorEntityToTutor(tutorEntity);
  }

  /**
   * Adds a new Tutor.
   * @param user Firebase user object.
   * @param tutor Tutor object.
   * @return Newly added Tutor object.
   * @throws BadRequestException
   * @throws UnauthorizedException
   * @throws InternalServerErrorException
   */
  @ApiMethod(
      name = MethodName.POST_TUTORS,
      path = PathName.POST_TUTORS,
      httpMethod = ApiMethod.HttpMethod.POST)
  public Tutor postTutor(final com.google.appengine.api.users.User user,
      Tutor tutor) throws BadRequestException, UnauthorizedException, InternalServerErrorException {

    authorize(user);

    validate(tutor);

    /* Construct a user entity key */
    Key userKey = new KeyFactory.Builder(User.Property.KIND, user.getUserId())
        .getKey();

    /* check for the user user entity that must be present in the database */
    try {
      getExistingEntity(userKey);
    } catch (com.google.api.server.spi.response.NotFoundException e) {
      throw new BadRequestException(Exception.STUDENT_PROFILE_REQUIRED);
    }

    /* Obtain a key of the requested student user entity */
    Key studentKey = generateStudentKey(user.getUserId());

    /* Obtain a student user entity that must be present in the database */
    Entity studentEntity;
    try {
      studentEntity = getExistingEntity(studentKey);
    } catch (com.google.api.server.spi.response.NotFoundException e) {
      throw new BadRequestException(Exception.STUDENT_PROFILE_REQUIRED);
    }

    /* Set the properties that are not allowed to be set by a user */
    tutor.setId(user.getUserId());
    tutor.setIsHidden(false);
    tutor.setIsSearchable(true);
    /* Assign the city, the province and the geopoint properties */
    geocodeTutorProperties(tutor);

    /*
      There must be no tutor user entity with the id equal
      to the provided one in the database
      */
    Key tutorKey = generateTutorKey(user.getUserId());
    validateEntityNonexistence(tutorKey);

    /* Obtain properties from the student user entity to create a tutor entity */
    String pictureURL = (String) studentEntity.getProperty(Student.Property.PICTURE_URL);
    tutor.setPictureURL(pictureURL);
    String email = (String) studentEntity.getProperty(Student.Property.EMAIL);
    tutor.setEmail(email);
    String firstName = (String) studentEntity.getProperty(Student.Property.FIRST_NAME);
    tutor.setFirstName(firstName);
    String lastName = (String) studentEntity.getProperty(Student.Property.LAST_NAME);
    tutor.setLastName(lastName);

    /* Create a new Tutor entity and assign it's properties */
    Entity tutorEntity = new Entity(Tutor.Property.KIND, tutor.getId(), userKey);
    tutorEntity.setUnindexedProperty(Tutor.Property.ID, tutor.getId());
    tutorEntity.setUnindexedProperty(Tutor.Property.PICTURE_URL, tutor.getPictureURL());
    tutorEntity.setUnindexedProperty(Tutor.Property.EMAIL, tutor.getEmail());
    tutorEntity.setUnindexedProperty(Tutor.Property.FIRST_NAME, tutor.getFirstName());
    tutorEntity.setUnindexedProperty(Tutor.Property.LAST_NAME, tutor.getLastName());
    tutorEntity.setUnindexedProperty(Tutor.Property.HEADLINE, tutor.getHeadline());
    tutorEntity.setUnindexedProperty(Tutor.Property.BIO, tutor.getBio());
    tutorEntity.setUnindexedProperty(Tutor.Property.POSTAL_CODE, tutor.getPostalCode());
    tutorEntity.setUnindexedProperty(Tutor.Property.CITY, tutor.getCity());
    tutorEntity.setUnindexedProperty(Tutor.Property.PROVINCE, tutor.getCity());
    tutorEntity.setIndexedProperty(Tutor.Property.GEO_POINT, tutor.getGeoPoint());
    tutorEntity.setIndexedProperty(Tutor.Property.RATE, tutor.getRate());
    tutorEntity.setIndexedProperty(Tutor.Property.COURSES, tutor.getCourses());
    tutorEntity.setIndexedProperty(Tutor.Property.ONLINE_LESSON, tutor.getIsOnlineLesson());
    tutorEntity.setIndexedProperty(Tutor.Property.IN_PERSON_LESSON, tutor.getIsInPersonLesson());
    tutorEntity.setUnindexedProperty(Tutor.Property.IS_HIDDEN, tutor.getIsHidden());

    /* Update student's entity type */
    studentEntity.setUnindexedProperty(Property.USER_TYPE, UserType.TUTOR);

    batchPut(Arrays.asList(studentEntity, tutorEntity));

    return tutor;
  }

  /**
   * Updates an existing Tutor entity in the database.
   * @param user Firebase user object.
   * @param tutor Tutor object.
   * @return Updated Tutor object.
   * @throws UnauthorizedException
   * @throws BadRequestException
   * @throws com.google.api.server.spi.response.NotFoundException
   * @throws InternalServerErrorException
   */
  @ApiMethod(
      name = MethodName.PUT_TUTORS,
      path = PathName.PUT_TUTORS,
      httpMethod = ApiMethod.HttpMethod.PUT)
  public Tutor putTutor(final com.google.appengine.api.users.User user, Tutor tutor)
      throws UnauthorizedException, BadRequestException,
      com.google.api.server.spi.response.NotFoundException, InternalServerErrorException {

    authorize(user);

    validate(tutor);

    /* Obtain a key of the requested tutor user entity from the database */
    Key tutorKey = generateTutorKey(user.getUserId());

    Entity tutorEntity = getExistingEntity(tutorKey);

    /* Decide whether student user will have to be updated */
    boolean isPictureURLUpdated = false;
    boolean isEmailUpdated = false;
    boolean isFirstNameUpdated = false;
    boolean isLastNameUpdated = false;
    boolean isHeadlineUpdated = false;

    if (tutor.getPictureURL() != null) {
      tutorEntity.setUnindexedProperty(Tutor.Property.PICTURE_URL, tutor.getPictureURL());
      isPictureURLUpdated = true;
    }

    if (tutor.getEmail() != null) {
      tutorEntity.setUnindexedProperty(Tutor.Property.EMAIL, tutor.getEmail());
      /* Update Firebase Auth account */
      // Send email verification client side when updating email.
      CreateRequest request = new CreateRequest().setEmail(tutor.getEmail()).setEmailVerified(false);
      try {
        FirebaseAuth.getInstance().createUser(request);
      } catch (FirebaseAuthException e) {
        e.printStackTrace();
        throw new BadRequestException(Exception.BAD_REQUEST);
      }
      isEmailUpdated = true;
    }

    if (tutor.getFirstName() != null) {
      tutorEntity.setUnindexedProperty(Tutor.Property.FIRST_NAME, tutor.getFirstName());
      isFirstNameUpdated = true;
    }

    if (tutor.getLastName() != null) {
      tutorEntity.setUnindexedProperty(Tutor.Property.LAST_NAME, tutor.getLastName());
      isLastNameUpdated = true;
    }

    if (tutor.getHeadline() != null) {
      tutorEntity.setUnindexedProperty(Tutor.Property.HEADLINE, tutor.getHeadline());
      isHeadlineUpdated = true;
    }

    if (tutor.getBio() != null) {
      tutorEntity.setUnindexedProperty(Tutor.Property.BIO, tutor.getBio());
    }

    if (tutor.getPostalCode() != null) {
      /* Re-compute the city, the province and the geopoint */
      geocodeTutorProperties(tutor);

      tutorEntity.setUnindexedProperty(Tutor.Property.POSTAL_CODE, tutor.getPostalCode());
      tutorEntity.setUnindexedProperty(Tutor.Property.CITY, tutor.getCity());
      tutorEntity.setUnindexedProperty(Tutor.Property.PROVINCE, tutor.getProvince());
      tutorEntity.setIndexedProperty(Tutor.Property.GEO_POINT, tutor.getGeoPoint());
    }

    if (tutor.getRate() != null) {
      tutorEntity.setIndexedProperty(Tutor.Property.RATE, tutor.getRate());
    }

    if (tutor.getCourses() != null) {
      tutorEntity.setIndexedProperty(Tutor.Property.COURSES, tutor.getCourses());
    }

    if (tutor.getIsHidden() != null) {
      tutorEntity.setUnindexedProperty(Tutor.Property.IS_HIDDEN, tutor.getIsHidden());
    }

    /* Checks if the tutor user entity can appear in the search results */
    tutorEntity.setIndexedProperty(Tutor.Property.IS_SEARCHABLE, isSearchable(tutorEntity));

    if (isPictureURLUpdated ||
        isEmailUpdated ||
        isFirstNameUpdated ||
        isLastNameUpdated ||
        isHeadlineUpdated) {

      /* Obtain a student user entity that must be present in the database */
      Key studentKey = generateStudentKey(user.getUserId());

      Entity studentEntity = getExistingEntity(studentKey);

      if (isPictureURLUpdated) {
        studentEntity.setUnindexedProperty(Property.PICTURE_URL, tutor.getPictureURL());
      }

      if (isEmailUpdated) {
        studentEntity.setUnindexedProperty(Student.Property.EMAIL, tutor.getEmail());
      }

      if (isFirstNameUpdated) {
        studentEntity.setUnindexedProperty(Student.Property.FIRST_NAME, tutor.getFirstName());
      }

      if (isLastNameUpdated) {
        studentEntity.setUnindexedProperty(Student.Property.LAST_NAME, tutor.getLastName());
      }

      if (isHeadlineUpdated) {
        studentEntity.setUnindexedProperty(Property.HEADLINE, tutor.getHeadline());
      }

      batchPut(Arrays.asList(studentEntity, tutorEntity));

    } else {
      /* Properties that are shared between student and tutor profiles have not been updated */
      putEntity(tutorEntity);
    }

    return convertTutorEntityToTutor(tutorEntity);
  }

  /**
   * Returns a list of Tutors.
   * @param user Firebase user object.
   * @param id Unique firebase ID of the Tutor.
   * @param properties List of selected Tutor properties.
   * @param lessonType Online or in person.
   * @param course Course taught by the Tutor.
   * @param city City in which the Tutor is located.
   * @param province Province in which the Tutor is located.
   * @param page String symbolizing how many pages of tutors to load.
   * @param perPage Number of tutors to display per page.
   * @return
   * @throws UnauthorizedException
   * @throws BadRequestException
   * @throws com.google.api.server.spi.response.NotFoundException
   * @throws InternalServerErrorException
   */
  @ApiMethod(
      name = MethodName.GET_SEARCH,
      path = PathName.GET_SEARCH,
      httpMethod = HttpMethod.GET)
  public CollectionResponse<Tutor> getSearch(final com.google.appengine.api.users.User user,
      @Named(SEARCH_QUERY_PROPERTY.ID) @Nullable String id,
      @Named(SEARCH_QUERY_PROPERTY.PROPERTIES) @Nullable List<String> properties,
      @Named(SEARCH_QUERY_PROPERTY.LESSON_TYPE) @Nullable String lessonType,
      @Named(SEARCH_QUERY_PROPERTY.COURSE) @Nullable String course,
      /* E.g. show all the tutors that are in the specified city and province. */
      @Named(SEARCH_QUERY_PROPERTY.CITY) @Nullable String city,
      @Named(SEARCH_QUERY_PROPERTY.PROVINCE) @Nullable String province,
      @Named(SEARCH_QUERY_PROPERTY.PAGE) @Nullable String page,
      @Named(SEARCH_QUERY_PROPERTY.PER_PAGE) @Nullable
      @DefaultValue(SEARCH_QUERY_PROPERTY.SEARCH_PER_PAGE_DEFAULT_VALUE) Integer perPage)
      throws UnauthorizedException, BadRequestException,
      com.google.api.server.spi.response.NotFoundException, InternalServerErrorException {

    authorize(user);

    if (id != null) {

      /* Return a list containing a single tutor user */

      /*
      Validate the query arguments.

      The query requesting a single tutor must not have the arguments
      for the requesting multiple tutors.
      */
      if (properties != null || lessonType != null || course != null || city != null
          || province != null || page != null || perPage != null) {
        throw new BadRequestException(Exception.BAD_REQUEST);
      }

      /* Construct a tutor entity key */
      Key tutorKey = generateTutorKey(id);

      /* Fetch a must existing tutor entity from the database */
      Entity tutorEntity = getExistingEntity(tutorKey);

      Tutor tutor = convertTutorEntityToTutor(tutorEntity);

      return CollectionResponse.<Tutor> builder()
          .setItems(Collections.singletonList(tutor))
          .build();

    } else {

      /* Return a list of tutors that are teaching the specified course */
      for (String property: properties) {
        if (!TutorProperty.contains(property)) {
          throw new BadRequestException(Exception.INVALID_TUTOR_PROPERTY_IN_THE_QUERY);
        }
      }

      /* Check if the courses argument is provided */
      if (course == null) {
        throw new BadRequestException(Exception.COURSE_REQUIRED_IN_THE_QUERY);
      }

      /* Validate the perPage argument */
      if (!Validator.isValidPerPage(perPage)) {
        throw new BadRequestException(Exception.INVALID_PER_PAGE_IN_THE_QUERY);
      }

      /* Check if the course argument is in the list of teaching courses. */
      List<String> courseList = Arrays.asList(course.split(","));
      try {
        validateCourses(courseList);
      } catch (BadRequestException e) {
        throw new BadRequestException(Exception.INVALID_COURSE_IN_THE_QUERY);
      }

      /* Prepare the query */
      Query tutorQuery = new Query(Tutor.Property.KIND);

      /* Show only the searchable tutors */
      tutorQuery.setFilter(new FilterPredicate(Tutor.Property.IS_SEARCHABLE, FilterOperator.EQUAL, true));

      /* Check if a user specified what properties to return */
      if (properties.size() == 0) {
        /* Default projection query */
        tutorQuery.
            addProjection(
                new PropertyProjection(TutorProperty.ID.getField(), String.class)).
            addProjection(
                new PropertyProjection(TutorProperty.PICTURE_URL.getField(), String.class)).
            addProjection(
                new PropertyProjection(TutorProperty.FIRST_NAME.getField(), String.class)).
            addProjection(
                new PropertyProjection(TutorProperty.LAST_NAME.getField(), String.class)).
            addProjection(
                new PropertyProjection(TutorProperty.RATING.getField(), Long.class)).
            addProjection(
                new PropertyProjection(TutorProperty.HEADLINE.getField(), String.class)).
            addProjection(
                new PropertyProjection(TutorProperty.CITY.getField(), String.class)).
            addProjection(
                new PropertyProjection(TutorProperty.PROVINCE.getField(), String.class)).
            addProjection(
                new PropertyProjection(TutorProperty.RATE.getField(), Long.class)).
            addProjection(
                new PropertyProjection(TutorProperty.COURSES.getField(), List.class));

      } else {
        /* Custom projection query */
        if (properties.contains(TutorProperty.ID.getField())) {
          tutorQuery.addProjection(new PropertyProjection(TutorProperty.ID.getField(), String.class));
        }
        if (properties.contains(TutorProperty.PICTURE_URL.getField())) {
          tutorQuery.addProjection(new PropertyProjection(TutorProperty.PICTURE_URL.getField(), String.class));
        }
        if (properties.contains(TutorProperty.EMAIL.getField())) {
          tutorQuery.addProjection(new PropertyProjection(TutorProperty.EMAIL.getField(), String.class));
        }
        if (properties.contains(TutorProperty.FIRST_NAME.getField())) {
          tutorQuery.addProjection(new PropertyProjection(TutorProperty.FIRST_NAME.getField(), String.class));
        }
        if (properties.contains(TutorProperty.LAST_NAME.getField())) {
          tutorQuery.addProjection(new PropertyProjection(TutorProperty.LAST_NAME.getField(), String.class));
        }
        if (properties.contains(TutorProperty.HEADLINE.getField())) {
          tutorQuery.addProjection(new PropertyProjection(TutorProperty.HEADLINE.getField(), String.class));
        }
        if (properties.contains(TutorProperty.BIO.getField())) {
          tutorQuery.addProjection(new PropertyProjection(TutorProperty.BIO.getField(), String.class));
        }
        if (properties.contains(TutorProperty.POSTAL_CODE.getField())) {
          tutorQuery.addProjection(new PropertyProjection(TutorProperty.POSTAL_CODE.getField(), String.class));
        }
        if (properties.contains(TutorProperty.CITY.getField())) {
          tutorQuery.addProjection(new PropertyProjection(TutorProperty.CITY.getField(), String.class));
        }
        if (properties.contains(TutorProperty.PROVINCE.getField())) {
          tutorQuery.addProjection(new PropertyProjection(TutorProperty.PROVINCE.getField(), String.class));
        }
        if (properties.contains(TutorProperty.GEO_POINT.getField())) {
          tutorQuery.addProjection(new PropertyProjection(TutorProperty.GEO_POINT.getField(), GeoPt.class));
        }
        if (properties.contains(TutorProperty.RATE.getField())) {
          tutorQuery.addProjection(new PropertyProjection(TutorProperty.RATE.getField(), Long.class));
        }
        if (properties.contains(TutorProperty.COURSES.getField())) {
          tutorQuery.addProjection(new PropertyProjection(TutorProperty.COURSES.getField(), List.class));
        }
        if (properties.contains(TutorProperty.LESSON_TYPE.getField())) {
          tutorQuery.addProjection(new PropertyProjection(TutorProperty.LESSON_TYPE.getField(), String.class));
        }
      }

      /* If city is and province are not provided we show results for Calgary, AB for now */
      if (city == null && province == null) {
        /* Use city and province for the search. The user is offered with a list of cities, thus
         * we do not check for the city and province validity here */
        tutorQuery.
            setFilter(new FilterPredicate(
                Tutor.Property.PROVINCE,
                FilterOperator.EQUAL,
                SEARCH_QUERY_PROPERTY.DEFAULT_PROVINCE
            ));
        tutorQuery.
            setFilter(new FilterPredicate(
                Tutor.Property.CITY,
                FilterOperator.EQUAL,
                SEARCH_QUERY_PROPERTY.DEFAULT_CITY
            ));
      } else {
        if (city != null) {
          tutorQuery.setFilter(new FilterPredicate(Tutor.Property.CITY, FilterOperator.EQUAL, city));
        }
        if (province != null) {
          tutorQuery.setFilter(new FilterPredicate(Tutor.Property.PROVINCE, FilterOperator.EQUAL, province));
        }
      }

      /* Sort the tutors in the descending order based on their rating */
      tutorQuery.addSort(Tutor.Property.RATING, SortDirection.DESCENDING);

      DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
      PreparedQuery preparedTutorQuery = datastore.prepare(tutorQuery);

      /* Check if perPage is optional */
      FetchOptions fetchOptions = FetchOptions.Builder.withLimit(perPage);
      if (page != null) {
        fetchOptions.startCursor(Cursor.fromWebSafeString(page));
      }

      List<Entity> tutorEntities;
      try {
        tutorEntities = preparedTutorQuery.asQueryResultList(fetchOptions);
      } catch (IllegalArgumentException e) {
        /* Invalid cursor */
        throw new BadRequestException(Exception.INVALID_PAGE_IN_THE_QUERY);
      }

      /* Convert the list of tutor entities to the list of tutors */
      ArrayList<Tutor> tutors = new ArrayList<>();
      for (Entity tutorEntity : tutorEntities) {
        tutors.add(convertTutorEntityToTutor(tutorEntity));
      }

      /* Construct the next cursor */
      String nextPageCursor = ((QueryResultList<Entity>) tutorEntities).getCursor().toWebSafeString();

      return CollectionResponse.<Tutor> builder()
          .setItems(tutors)
          .setNextPageToken(nextPageCursor)
          .build();
    }
  }

  /**
   * Returns a list of courses.
   * @param user Firebase user object.
   * @param page String representing current position in list of courses.
   * @param perPage Number of courses to load.
   * @return A collection containing the list of courses and a cursor representing the next page of courses.
   * @throws BadRequestException
   * @throws UnauthorizedException
   * @throws InternalServerErrorException
   */
  @ApiMethod(
      name = MethodName.GET_COURSES,
      path = PathName.GET_COURSES,
      httpMethod = HttpMethod.GET)
  public CollectionResponse<Course> getCourses(final com.google.appengine.api.users.User user,
      @Named(SEARCH_QUERY_PROPERTY.PAGE) @Nullable String page,
      @Named(SEARCH_QUERY_PROPERTY.PER_PAGE) @Nullable
      @DefaultValue(SEARCH_QUERY_PROPERTY.SUBJECTS_PER_PAGE_DEFAULT_VALUE) Integer perPage)
      throws BadRequestException, UnauthorizedException, InternalServerErrorException {

    authorize(user);

    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    Query courseQuery = new Query(Course.Property.KIND);
    PreparedQuery preparedCourseQuery = datastore.prepare(courseQuery);

    FetchOptions fetchOptions = FetchOptions.Builder.withLimit(perPage);
    if (page != null) {
      fetchOptions.startCursor(Cursor.fromWebSafeString(page));
    }

    List<Entity> subjectEntities;
    try {
      subjectEntities = preparedCourseQuery.asQueryResultList(fetchOptions);
    } catch (IllegalArgumentException e) {
      /* Invalid cursor */
      throw new BadRequestException(Exception.INVALID_PAGE_IN_THE_QUERY);
    }

    /* Convert the list of course entities to the array list of courses */
    ArrayList<Course> courseArray = new ArrayList<>();
    for (Entity subjectEntity : subjectEntities) {
      courseArray.add(convertCourseEntityToCourse(subjectEntity));
    }

    /* Construct a new cursor */
    String nextPageCursor = ((QueryResultList<Entity>) subjectEntities).getCursor().toWebSafeString();

    return CollectionResponse.<Course> builder()
        .setItems(courseArray)
        .setNextPageToken(nextPageCursor)
        .build();
  }

  /**
   * Returns the payment method.
   * @param user Firebase user object.
   * @return PaymentMethod object.
   * @throws BadRequestException
   * @throws UnauthorizedException
   * @throws NotFoundException
   * @throws InternalServerErrorException
   */
  @ApiMethod(
      name = MethodName.GET_PAYMENT,
      path = PathName.GET_PAYMENT,
      httpMethod = HttpMethod.GET)
  public PaymentMethod getPaymentMethod(
      final com.google.appengine.api.users.User user)
      throws BadRequestException, UnauthorizedException, NotFoundException,
      InternalServerErrorException {

    authorize(user);

    /* Get customer Id */
    /* Construct a student entity key */
    Key studentKey = generateStudentKey(user.getUserId());
    /* Fetch a must existing student entity from the database */
    Entity studentEntity = getExistingEntity(studentKey);
    String customerId = (String) studentEntity.getProperty(Property.STRIPE_ID);
    String cardId = (String) studentEntity.getProperty(Property.CARD_ID);

    PaymentMethod paymentMethod;
    try {
      Customer customer = Customer.retrieve(customerId);
      Card card = (Card) customer.getSources().retrieve(cardId);
      if (card == null) {
        throw new NotFoundException(Exception.NOT_FOUND);
      }
      String tokenId = card.getId();
      String brand = card.getBrand();
      String last4 = card.getLast4();
      String expirationMonth = Integer.toString(card.getExpMonth());
      String expirationYear = Integer.toString(card.getExpYear());
      paymentMethod = new PaymentMethod(tokenId, brand, last4, expirationMonth, expirationYear);
    } catch (AuthenticationException | APIConnectionException | APIException e) {
      e.printStackTrace();
      throw new InternalServerErrorException(Exception.INTERNAL);
    } catch (InvalidRequestException e) {
      e.printStackTrace();
      throw new NotFoundException(Exception.NOT_FOUND);
    } catch (CardException e) {
      e.printStackTrace();
      throw new BadRequestException(Exception.BAD_REQUEST);
    }

    return paymentMethod;
  }

  /**
   * Adds a new payment method.
   * @param user Firebase user object.
   * @param paymentMethod PaymentMethod object.
   * @return Newly created PaymentMethod object.
   * @throws BadRequestException
   * @throws UnauthorizedException
   * @throws NotFoundException
   * @throws InternalServerErrorException
   */
  @ApiMethod(
      name = MethodName.POST_PAYMENT,
      path = PathName.POST_PAYMENT,
      httpMethod = HttpMethod.POST)
  public PaymentMethod postPaymentMethod(
      final com.google.appengine.api.users.User user, PaymentMethod paymentMethod)
      throws BadRequestException, UnauthorizedException, NotFoundException,
      InternalServerErrorException {

    authorize(user);

    /* Construct a student entity key */
    Key studentKey = generateStudentKey(user.getUserId());
    /* Fetch a must existing student entity from the database */
    Entity studentEntity = getExistingEntity(studentKey);

    /* Return a :Student constructed from the student entity */
    String customerId = (String) studentEntity.getProperty(Property.STRIPE_ID);

    Customer customer;
    try {
      /* Get the customer from Stripe */
      customer = Customer.retrieve(customerId);
      Map<String, Object> parameters = new HashMap<>();
      /* Delete the old card */
      String tokenId = (String) studentEntity.getProperty(Property.CARD_ID);
      if (tokenId != null) {
        customer.getSources().retrieve(tokenId).delete();
      }
      /* Add the new card */
      parameters.put("source", paymentMethod.getTokenId());
      Card card = (Card) customer.getSources().create(parameters);
      /* Add the new card id to the database */
      studentEntity.setUnindexedProperty(Property.CARD_ID, card.getId());
      batchPut(Collections.singletonList(studentEntity));
      /* Return the new card */
      paymentMethod.setTokenId(null);
      paymentMethod.setLast4(card.getLast4());
    } catch (AuthenticationException | InvalidRequestException | APIConnectionException | APIException e) {
      e.printStackTrace();
      throw new InternalServerErrorException(Exception.INTERNAL);
    } catch (CardException e) {
      e.printStackTrace();
      throw new BadRequestException(Exception.INVALID_PAYMENT_METHOD);
    }

    return paymentMethod;
  }

  /**
   * Deletes the payment method.
   * @param user Firebase user object.
   * @param paymentMethod PaymentMethod object.
   * @throws BadRequestException
   * @throws UnauthorizedException
   * @throws NotFoundException
   * @throws InternalServerErrorException
   */
  @ApiMethod(
      name = MethodName.DELETE_PAYMENT,
      path = PathName.DELETE_PAYMENT,
      httpMethod = HttpMethod.DELETE)
  public void deletePaymentMethod(
      final com.google.appengine.api.users.User user, PaymentMethod paymentMethod)
      throws BadRequestException, UnauthorizedException, NotFoundException,
      InternalServerErrorException {

    authorize(user);

    /* Get customer Id */
    /* Construct a student entity key */
    Key studentKey = generateStudentKey(user.getUserId());
    /* Fetch a must existing student entity from the database */
    Entity studentEntity = getExistingEntity(studentKey);

    /* Return a :Student constructed from the student entity */
    String customerId = (String) studentEntity.getProperty(Property.STRIPE_ID);

    Customer customer;
    try {
      customer = Customer.retrieve(customerId);
      customer.getSources().retrieve(paymentMethod.getTokenId()).delete();
      studentEntity.setUnindexedProperty(Property.CARD_ID, null);
      batchPut(Collections.singletonList(studentEntity));
    } catch (
        AuthenticationException |
        InvalidRequestException |
        APIConnectionException  |
        APIException e) {
      e.printStackTrace();
      throw new InternalServerErrorException(Exception.INTERNAL);
    } catch (CardException e) {
      e.printStackTrace();
      throw new BadRequestException(Exception.INVALID_PAYMENT_METHOD);
    }
  }

  /**
   * Converts a course entity to a Course object.
   * @param courseEntity Course entity.
   * @return Course object.
   */
  private Course convertCourseEntityToCourse(Entity courseEntity) {
    String name = (String) courseEntity.getProperty(Course.Property.NAME);
    String subject = (String) courseEntity.getProperty(Course.Property.SUBJECT);
    return new Course(name, subject);
  }

  /**
   * Atomically adds entities to the database.
   * @param entities List of entities.
   * @throws InternalServerErrorException
   */
  private void batchPut(List<Entity> entities)
      throws InternalServerErrorException {

    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    Transaction transaction = datastore.beginTransaction();

    try {
      datastore.put(transaction, entities);
      transaction.commit();
    } catch (java.lang.Exception exception) {
      throw new InternalServerErrorException(Exception.INTERNAL);
    } finally {
      if (transaction.isActive()) {
        transaction.rollback();
      }
    }
  }

  /**
   * Validates newly created Student properties.
   * @param student Student object.
   * @throws BadRequestException
   */
  private void validate(Student student) throws BadRequestException {
    if (student.getEmail() != null && !Validator.isValidEmail(student.getEmail())) {
      throw new BadRequestException(Exception.INVALID_EMAIL);
    }
    if (student.getFirstName() != null && !Validator.isValidName(student.getFirstName())) {
      throw new BadRequestException(Exception.INVALID_FIRST_NAME);
    }
    if (student.getLastName() != null && !Validator.isValidName(student.getLastName())) {
      throw new BadRequestException(Exception.INVALID_LAST_NAME);
    }
    if (student.getHeadline() != null && !Validator.isValidHeadline(student.getHeadline())) {
      throw new BadRequestException(Exception.INVALID_HEADLINE);
    }
  }


  /**
   * Validates Tutor properties.
   * @param tutor Tutor object.
   * @throws BadRequestException
   */
  private void validate(Tutor tutor) throws BadRequestException {
    if (tutor.getEmail() != null && !Validator.isValidEmail(tutor.getEmail())) {
      throw new BadRequestException(Exception.INVALID_EMAIL);
    }
    if (tutor.getFirstName() != null && !Validator.isValidName(tutor.getFirstName())) {
      throw new BadRequestException(Exception.INVALID_FIRST_NAME);
    }
    if (tutor.getLastName() != null && !Validator.isValidName(tutor.getLastName())) {
      throw new BadRequestException(Exception.INVALID_LAST_NAME);
    }
    if (tutor.getHeadline() != null && !Validator.isValidHeadline(tutor.getHeadline())) {
      throw new BadRequestException(Exception.INVALID_HEADLINE);
    }
    if (tutor.getBio() != null && !Validator.isValidBio(tutor.getBio())) {
      throw new BadRequestException(Exception.INVALID_BIO);
    }
    if (tutor.getPostalCode() != null && !Validator.isValidPostalCode(tutor.getPostalCode())) {
      throw new BadRequestException(Exception.INVALID_POSTAL_CODE);
    }
    if (tutor.getRate() != null && !Validator.isValidRate(tutor.getRate())) {
      throw new BadRequestException(Exception.INVALID_RATE);
    }
    if (tutor.getCourses() != null && tutor.getCourses().size() != 0) {
      validateCourses(tutor.getCourses());
    }
  }

  /**
   * Validate Courses.
   * @param courses List of courses.
   * @throws BadRequestException
   */
  private void validateCourses(List<String> courses) throws BadRequestException {
    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    for (String course : courses) {
      Query courseQuery = new Query(Course.Property.KIND)
          .setFilter(new FilterPredicate(Course.Property.NAME, FilterOperator.EQUAL, course));
      PreparedQuery preparedCourseQuery = datastore.prepare(courseQuery);
      if (preparedCourseQuery.asSingleEntity() == null) {
        throw new BadRequestException(Exception.INVALID_COURSE);
      }
    }
  }

  /**
   * Converts the postal code to an address (e.g. "Calgary, AB") and a geographic point.
   * @param tutor Tutor object.
   * @throws BadRequestException
   * @throws InternalServerErrorException
   */
  private void geocodeTutorProperties(Tutor tutor)
      throws BadRequestException, InternalServerErrorException {
    GeocodingResult[] results = geocode(tutor.getPostalCode());

    if (results == null) {
      throw new BadRequestException(Exception.INVALID_POSTAL_CODE);
    }

    /* The city associated with the geopoint e.g. "Calgary" */
    String city = results[0].addressComponents[2].shortName;
    tutor.setCity(city);

    /* The state or province associated with the geopoint e.g. "AB" */
    String province = results[0].addressComponents[4].shortName;
    tutor.setProvince(province);

    float latitude = (float) results[0].geometry.location.lat;
    float longitude = (float) results[0].geometry.location.lng;
    GeoPt geopoint = new GeoPt(latitude, longitude);
    tutor.setGeoPoint(geopoint);
  }

  /**
   * Calls Google Geocoding API to convert the geographic point to a human readable address.
   * @param address String representation of a street address.
   * @return
   * @throws InternalServerErrorException
   */
  private GeocodingResult[] geocode(String address) throws InternalServerErrorException {
    String apiKey = System.getenv(Constant.GOOGLE_GEOCODING_API_KEY);

    /* Call Google Geocoding API */
    GeoApiContext context = new GeoApiContext.Builder(new GaeRequestHandler.Builder())
        .apiKey(apiKey)
        .build();

    GeocodingResult[] results;
    try {
      results = GeocodingApi.geocode(context, address).await();
    } catch (ApiException | InterruptedException | IOException e) {
      logger.log(Level.WARNING, Exception.INTERNAL);
      throw new InternalServerErrorException(Exception.INTERNAL);
    }

    return results;
  }

  /**
   * Checks if the tutor entity can appear in the search results.
   * @param tutorEntity Tutor entity.
   * @return boolean value of whether or not the Tutor is searchable.
   */
  private Boolean isSearchable(Entity tutorEntity) {
    String pictureURL = (String) tutorEntity.getProperty(Tutor.Property.PICTURE_URL);
    String firstName = (String) tutorEntity.getProperty(Tutor.Property.FIRST_NAME);
    String lastName = (String) tutorEntity.getProperty(Tutor.Property.LAST_NAME);
    String headline = (String) tutorEntity.getProperty(Tutor.Property.HEADLINE);
    String postalCode = (String) tutorEntity.getProperty(Tutor.Property.POSTAL_CODE);
    Boolean hidden = (Boolean) tutorEntity.getProperty(Tutor.Property.IS_HIDDEN);
    Long rate = (Long) tutorEntity.getProperty(Tutor.Property.RATE);
    @SuppressWarnings("unchecked")
    ArrayList<String> courses = (ArrayList<String>) tutorEntity.getProperty(Tutor.Property.COURSES);
    Boolean onlineLesson = (Boolean) tutorEntity.getProperty(Tutor.Property.ONLINE_LESSON);
    Boolean inPersonLesson = (Boolean) tutorEntity.getProperty(Tutor.Property.IN_PERSON_LESSON);

    return (pictureURL != null)                           &&
        (firstName != null)                               &&
        (lastName != null)                                &&
        (headline != null)                                &&
        (postalCode != null)                              &&
        (!hidden)                                         &&
        (onlineLesson != null || inPersonLesson != null)  &&
        (rate != null)                                    &&
        (courses.size() != 0);
  }

  /**
   * Checks if the user is authorized.
   * @param user Firebase user object.
   * @throws UnauthorizedException
   * @throws InternalServerErrorException
   */
  private void authorize(com.google.appengine.api.users.User user)
      throws UnauthorizedException, InternalServerErrorException {
    if (user == null) {
      throw new UnauthorizedException(Exception.UNAUTHORIZED);
    }
    try {
      UserRecord userRecord = FirebaseAuth.getInstance().getUser(user.getUserId());
      /* Checks if the user's email is verified */
      if (isProduction) {
        if (!userRecord.isEmailVerified()) {
          throw new UnauthorizedException(Exception.EMAIL_CONFIRMATION_REQUIRED);
        }
      }
      /* Checks if the user's account is disabled */
      if (userRecord.isDisabled()) {
        throw new UnauthorizedException(Exception.DISABLED_ACCOUNT);
      }
    } catch (FirebaseAuthException e) {
      e.printStackTrace();
      throw new InternalServerErrorException(Exception.INTERNAL);
    }
  }

  /**
   * Returns a previously existing entity from the database.
   * @param key Datastore key of existing entity.
   * @return Datastore entity.
   * @throws com.google.api.server.spi.response.NotFoundException
   */
  private Entity getExistingEntity(Key key) throws
      com.google.api.server.spi.response.NotFoundException {
    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    try {
      return datastore.get(key);
    } catch (EntityNotFoundException exception) {
      /* Entity with the provided key doesn't exist in the database */
      throw new com.google.api.server.spi.response.NotFoundException(Exception.NOT_FOUND);
    }
  }

  /**
   * Checks if an entity exists in the database.
   * @param key  Datastore key.
   * @throws BadRequestException
   */
  private void validateEntityNonexistence(Key key) throws BadRequestException {
    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    try {
      Entity entity = datastore.get(key);
      if (entity != null) {
        /* Entity with provided id already exists in the database */
        throw new BadRequestException(Exception.BAD_REQUEST);
      }
    } catch (EntityNotFoundException exception) {
      /*
      Expected Behavior. Entity with the provided key doesn't exist in the database.
       */
    }
  }

  /**
   * Converts a Tutor entity to a Tutor instance.
   * @param tutorEntity Datastore Tutor entity.
   * @return Tutor object.
   */
  private Tutor convertTutorEntityToTutor(Entity tutorEntity) {
    String id = (String) tutorEntity.getProperty(Tutor.Property.ID);
    String pictureURL = (String) tutorEntity.getProperty(Tutor.Property.PICTURE_URL);
    String email = (String) tutorEntity.getProperty(Tutor.Property.EMAIL);
    String firstName = (String) tutorEntity.getProperty(Tutor.Property.FIRST_NAME);
    String lastName = (String) tutorEntity.getProperty(Tutor.Property.LAST_NAME);
    Double rating = (Double) tutorEntity.getProperty(Tutor.Property.RATING);
    String headline = (String) tutorEntity
        .getProperty(Tutor.Property.HEADLINE);
    String bio = (String) tutorEntity.getProperty(Tutor.Property.BIO);
    String postalCode = (String) tutorEntity.getProperty(Tutor.Property.POSTAL_CODE);
    String city = (String) tutorEntity.getProperty(Tutor.Property.CITY);
    String province = (String) tutorEntity.getProperty(Tutor.Property.PROVINCE);
    GeoPt geopoint = (GeoPt) tutorEntity.getProperty(Tutor.Property.GEO_POINT);
    Integer rate = (Integer) tutorEntity.getProperty(Tutor.Property.RATE);
    @SuppressWarnings("unchecked")
    List<String> coursesProperty =
        (List<String>) tutorEntity.getProperty(Tutor.Property.COURSES);
    Boolean onlineLesson =
        (Boolean) tutorEntity.getProperty(Tutor.Property.ONLINE_LESSON);
    Boolean inPersonLesson =
        (Boolean) tutorEntity.getProperty(Tutor.Property.IN_PERSON_LESSON);
    Boolean hidden =
        (Boolean) tutorEntity.getProperty(Tutor.Property.IS_HIDDEN);
    Boolean searchable = (Boolean) tutorEntity.getProperty(Tutor.Property.IS_SEARCHABLE);

    return new Tutor(id, pictureURL, email, firstName, lastName, rating,
        headline, bio, postalCode, city, province, geopoint, rate, coursesProperty, onlineLesson,
        inPersonLesson, hidden, searchable);
  }

  /**
   * Converts a Student entity to a Student instance.
   * @param studentEntity Datastore student entity.
   * @return Student object.
   */
  private Student convertStudentEntityToStudent(Entity studentEntity) {
    String id = (String) studentEntity.getProperty(Property.ID);
    String pictureURL = (String) studentEntity.getProperty(Property.PICTURE_URL);
    String email = (String) studentEntity.getProperty(Property.EMAIL);
    String firstName = (String) studentEntity.getProperty(Property.FIRST_NAME);
    String lastName = (String) studentEntity.getProperty(Property.LAST_NAME);
    String headline =
        (String) studentEntity.getProperty(Property.HEADLINE);
    String defaultPaymentMethod =
        (String) studentEntity.getProperty(Property.DEFAULT_PAYMENT_METHOD);
    String userType = (String) studentEntity.getProperty(Property.USER_TYPE);

    return new Student(id, pictureURL, email, firstName, lastName, headline, defaultPaymentMethod,
        userType);
  }

  /**
   * Generates a datastore key for a student.
   * @param id Firebase ID of the user.
   * @return Newly generated Key object for the student.
   */
  private Key generateStudentKey(String id){
    Key studentKey = new KeyFactory.Builder(User.Property.KIND, id)
            .addChild(Student.Property.KIND, id)
            .getKey();
    return studentKey;
  }

  /**
   * Generates a datastore key for a tutor.
   * @param id Firebase ID of the user.
   * @return Newly generated Key object for the student.
   */
  private Key generateTutorKey(String id){
    Key tutorKey = new KeyFactory.Builder(User.Property.KIND, id)
            .addChild(Tutor.Property.KIND, id)
            .getKey();
    return tutorKey;
  }

}