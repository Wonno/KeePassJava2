package org.linguafranca.pwdb.keepasshttp;

import org.linguafranca.pwdb.Database;
import org.linguafranca.pwdb.Entry;
import org.linguafranca.pwdb.keepasshttp.Message.ResponseEntry;

import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Contains message processors for processing messages (doh)
 *
 * @author jo
 */
class Processor {

    private interface MessageProcessor {
        void process(Message.Request request, Message.Response response);
    }

    private final Database database;
    private final PwGenerator pwGenerator;
    private Map<String, MessageProcessor> processors = new HashMap<>();


    Processor(Database database, PwGenerator pwGenerator) {
        this.database = database;
        this.pwGenerator = pwGenerator;
        processors.put(Message.Type.TEST_ASSOCIATE, new TestAssociate());
        processors.put(Message.Type.ASSOCIATE, new Associate());
        processors.put(Message.Type.GET_LOGINS, new GetLogins());
        processors.put(Message.Type.GET_LOGINS_COUNT, new GetLoginsCount());
        processors.put(Message.Type.GET_ALL_LOGINS, new GetAllLogins());
        processors.put(Message.Type.SET_LOGIN, new SetLogin());
        processors.put(Message.Type.GENERATE_PASSWORD, new GeneratePassword());
    }

    void process(Message.Request request, Message.Response response) {
        processors.get(request.RequestType).process(request, response);
    }

    private String makeId() {
        return database.getName() + " (" + database.getRootGroup().getUuid().toString() + ")";
    }

    private class GetLogins implements MessageProcessor {
        public void process(final Message.Request r, Message.Response resp) {

            @SuppressWarnings("unchecked")
            List<Entry> entries = database.findEntries(new Entry.Matcher() {
                @Override
                public boolean matches(Entry entry) {
                    return entry.getUrl().startsWith(r.Url) || r.Url.startsWith(entry.getUrl());
                }
            });

            for (Entry entry : entries) {
                resp.Entries.add(new ResponseEntry(entry.getTitle(), entry.getUsername(), entry.getPassword(), entry.getUuid().toString()));
            }
            resp.Count = resp.Entries.size();
            resp.Id = makeId();
            resp.Success = true;
        }
    }

    private class GetLoginsCount implements MessageProcessor {
        public void process(Message.Request r, Message.Response resp) {
            processors.get(Message.Type.GET_LOGINS).process(r, resp);
            resp.Entries = null;
        }
    }

    private class GeneratePassword implements MessageProcessor {
        public void process(Message.Request r, Message.Response resp) {
            String p = pwGenerator.generate();
            resp.Entries.add(new ResponseEntry("Password", "login", p, UUID.randomUUID().toString()));
            resp.Count = resp.Entries.size();
            resp.Id = makeId();
            resp.Success = true;
        }
    }

    private class GetAllLogins implements MessageProcessor {
        public void process(Message.Request r, Message.Response resp) {
            @SuppressWarnings("unchecked")
            List<Entry> entries = database.findEntries(new Entry.Matcher() {
                @Override
                public boolean matches(Entry entry) {
                    return true;
                }
            });
            for (Entry entry : entries) {
                resp.Entries.add(new ResponseEntry(entry.getTitle(), entry.getUsername(), entry.getPassword(), entry.getUuid().toString()));
            }
            resp.Count = resp.Entries.size();
            resp.Id = makeId();
            resp.Success = true;
        }
    }

    private static SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private class SetLogin implements MessageProcessor {
        public void process(final Message.Request r, Message.Response resp) {
            Entry entry = null;
            if (r.Uuid != null) {
                List entries = database.findEntries(new Entry.Matcher() {
                    @Override
                    public boolean matches(Entry entry) {
                        return entry.getUuid().toString().equals(r.Uuid);
                    }
                });
                entry = (Entry) entries.get(0);
            }
            if (entry == null) {
                entry = database.newEntry();
                entry.setTitle("New Entry " + format.format(new Date()));
            }
            entry.setPassword(r.Password);
            entry.setUsername(r.Login);
            entry.setUrl(r.Url);
            entry.setProperty("SubmitUrl", r.SubmitUrl);
            entry.setNotes("Created automatically");
            //noinspection unchecked
            database.getRootGroup().addEntry(entry);
            resp.Id = makeId();
            resp.Success = true;
        }
    }

    private class Associate implements MessageProcessor {
        @Override
        public void process(Message.Request request, Message.Response response) {
            response.Id = makeId();
            response.Success = true;
        }

    }

    private class TestAssociate implements MessageProcessor {
        @Override
        public void process(Message.Request request, Message.Response response) {
            response.Success = false;
            if (request.Id != null) {
                response.Success = request.Id.equals(makeId());
                response.Id = makeId();
            }
        }

    }
}
