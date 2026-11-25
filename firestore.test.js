const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const SPAIN_UID = "spain_user_1";
const NIGERIA_UID = "nigeria_user_2";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

function pastDate(offsetMs = 5000) {
  return new Date(Date.now() - offsetMs);
}

test("Unauthenticated user: denied all reads and writes", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(SPAIN_UID).get());
  await assertFails(unauthDb.collection("emergency_reports").where("country", "==", "Spain").get());
  await assertFails(
    unauthDb.collection("emergency_reports").doc("rep_1").set({
      userId: SPAIN_UID,
      emergencyType: "RESCUE",
      status: "ACTIVE",
      country: "Spain",
      region: "Madrid, Comunidad de",
      localArea: "Madrid",
      latitude: 40.4168,
      longitude: -3.7038,
      reportedTime: "12:00:00",
      reportedDate: "2026-11-25",
      testAlert: false,
      discreetMode: false,
      createdAt: pastDate(),
      updatedAt: pastDate(),
    })
  );
});

test("UserProfile: owner create/update/get succeeds; cross-user PII read and invalid country fail", async () => {
  const spainDb = testEnv.authenticatedContext(SPAIN_UID).firestore();
  const nigeriaDb = testEnv.authenticatedContext(NIGERIA_UID).firestore();
  const created = pastDate(10000);

  await assertSucceeds(
    spainDb.collection("users").doc(SPAIN_UID).set({
      userId: SPAIN_UID,
      firstName: "Elena",
      email: "elena@example.com",
      country: "Spain",
      region: "Madrid, Comunidad de",
      province: "Madrid",
      localArea: "Madrid",
      language: "es",
      createdAt: created,
      updatedAt: created,
    })
  );

  // Partial update allowed
  await assertSucceeds(
    spainDb.collection("users").doc(SPAIN_UID).update({
      firstName: "Elena M.",
      updatedAt: pastDate(1000),
    })
  );

  // Cross-user read (email PII protection) must fail
  await assertFails(nigeriaDb.collection("users").doc(SPAIN_UID).get());
  // Blanket list on users must fail
  await assertFails(spainDb.collection("users").get());
  // Unsupported country must fail
  await assertFails(
    nigeriaDb.collection("users").doc(NIGERIA_UID).set({
      userId: NIGERIA_UID,
      firstName: "Pierre",
      email: "pierre@example.com",
      country: "France",
      region: "Ile-de-France",
      localArea: "Paris",
      language: "en",
      createdAt: created,
      updatedAt: created,
    })
  );
  // Ghost field & @DocumentId 'id' rejected
  await assertFails(
    nigeriaDb.collection("users").doc(NIGERIA_UID).set({
      id: NIGERIA_UID,
      userId: NIGERIA_UID,
      firstName: "Osas",
      email: "osas@example.com",
      country: "Nigeria",
      region: "Edo",
      localArea: "Oredo",
      language: "en",
      createdAt: created,
      updatedAt: created,
    })
  );
});

test("RegisteredDevice: strictly isolated to owning user", async () => {
  const nigeriaDb = testEnv.authenticatedContext(NIGERIA_UID).firestore();
  const spainDb = testEnv.authenticatedContext(SPAIN_UID).firestore();
  const created = pastDate(10000);

  await assertSucceeds(
    nigeriaDb.collection("users").doc(NIGERIA_UID).collection("devices").doc("dev_1").set({
      userId: NIGERIA_UID,
      deviceName: "Osas Pixel 8",
      deviceModel: "Google Pixel 8",
      country: "Nigeria",
      latitude: 6.335,
      longitude: 5.6037,
      status: "ACTIVE",
      lastLocationUpdateText: "2026-11-25 12:15:00",
      createdAt: created,
      updatedAt: created,
    })
  );

  await assertSucceeds(
    nigeriaDb.collection("users").doc(NIGERIA_UID).collection("devices").get()
  );

  await assertSucceeds(
    nigeriaDb.collection("users").doc(NIGERIA_UID).collection("devices").doc("dev_1").update({
      latitude: 6.34,
      longitude: 5.61,
      status: "OFFLINE_LAST_KNOWN",
      lastLocationUpdateText: "2026-11-25 12:20:00",
      updatedAt: pastDate(1000),
    })
  );

  // Another user cannot read or list someone else's phone tracker
  await assertFails(spainDb.collection("users").doc(NIGERIA_UID).collection("devices").get());
  await assertFails(spainDb.collection("users").doc(NIGERIA_UID).collection("devices").doc("dev_1").get());
});

test("EmergencyReport: country isolation, filtered list, live GPS update, and false-alarm terminal lock", async () => {
  const spainDb = testEnv.authenticatedContext(SPAIN_UID).firestore();
  const nigeriaDb = testEnv.authenticatedContext(NIGERIA_UID).firestore();
  const created = pastDate(10000);

  // Seed user profiles
  await assertSucceeds(
    spainDb.collection("users").doc(SPAIN_UID).set({
      userId: SPAIN_UID,
      firstName: "Elena",
      email: "elena@example.com",
      country: "Spain",
      region: "Andalucía",
      province: "Sevilla",
      localArea: "Sevilla",
      language: "es",
      createdAt: created,
      updatedAt: created,
    })
  );
  await assertSucceeds(
    nigeriaDb.collection("users").doc(NIGERIA_UID).set({
      userId: NIGERIA_UID,
      firstName: "Osas",
      email: "osas@example.com",
      country: "Nigeria",
      region: "Edo",
      localArea: "Oredo",
      language: "en",
      createdAt: created,
      updatedAt: created,
    })
  );

  // Spain user cannot create Nigeria emergency report
  await assertFails(
    spainDb.collection("emergency_reports").doc("cross_rep").set({
      userId: SPAIN_UID,
      emergencyType: "RESCUE",
      status: "ACTIVE",
      country: "Nigeria",
      region: "Edo",
      localArea: "Oredo",
      latitude: 6.335,
      longitude: 5.6037,
      reportedTime: "12:00:00",
      reportedDate: "2026-11-25",
      testAlert: false,
      discreetMode: false,
      createdAt: created,
      updatedAt: created,
    })
  );

  // Nigeria user creates valid ROBBERY report (discreetMode = true)
  await assertSucceeds(
    nigeriaDb.collection("emergency_reports").doc("ng_rep_1").set({
      userId: NIGERIA_UID,
      emergencyType: "ROBBERY",
      status: "ACTIVE",
      country: "Nigeria",
      region: "Edo",
      localArea: "Oredo",
      latitude: 6.335,
      longitude: 5.6037,
      reportedTime: "12:05:00",
      reportedDate: "2026-11-25",
      testAlert: false,
      discreetMode: true,
      createdAt: created,
      updatedAt: created,
    })
  );

  // Filtered country query succeeds; unfiltered query fails
  await assertSucceeds(
    nigeriaDb.collection("emergency_reports").where("country", "==", "Nigeria").get()
  );
  await assertFails(nigeriaDb.collection("emergency_reports").get());

  // Spain user cannot .get() Nigeria report directly
  await assertFails(spainDb.collection("emergency_reports").doc("ng_rep_1").get());

  // Creator can update live GPS while ACTIVE
  await assertSucceeds(
    nigeriaDb.collection("emergency_reports").doc("ng_rep_1").update({
      latitude: 6.339,
      longitude: 5.608,
      updatedAt: pastDate(2000),
    })
  );

  // Non-creator cannot cancel or update another user's report
  await assertFails(
    spainDb.collection("emergency_reports").doc("ng_rep_1").update({
      status: "CANCELLED_FALSE_ALARM",
      updatedAt: pastDate(1500),
    })
  );

  // Creator cancels false alarm
  await assertSucceeds(
    nigeriaDb.collection("emergency_reports").doc("ng_rep_1").update({
      status: "CANCELLED_FALSE_ALARM",
      note: "False alarm closed by reporter",
      updatedAt: pastDate(1000),
    })
  );

  // Terminal state locking: cannot mutate after CANCELLED_FALSE_ALARM
  await assertFails(
    nigeriaDb.collection("emergency_reports").doc("ng_rep_1").update({
      latitude: 6.34,
      longitude: 5.61,
      updatedAt: pastDate(500),
    })
  );

  // Future timestamp rejected
  await assertFails(
    nigeriaDb.collection("emergency_reports").doc("ng_rep_future").set({
      userId: NIGERIA_UID,
      emergencyType: "KIDNAPPING",
      status: "ACTIVE",
      country: "Nigeria",
      region: "Edo",
      localArea: "Oredo",
      latitude: 6.335,
      longitude: 5.6037,
      reportedTime: "12:10:00",
      reportedDate: "2026-11-25",
      testAlert: true,
      discreetMode: false,
      createdAt: new Date(Date.now() + 60000),
      updatedAt: new Date(Date.now() + 60000),
    })
  );
});
