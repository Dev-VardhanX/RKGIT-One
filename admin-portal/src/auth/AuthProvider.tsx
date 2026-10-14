import {
  createContext,
  useContext,
  useEffect,
  useState,
  type ReactNode,
} from "react";
import type { Session } from "@supabase/supabase-js";
import { supabase } from "@/lib/supabase";

interface AuthContextType {
  session: Session | null;
  loading: boolean;
  isAdmin: boolean;
  signIn: (email: string, password: string) => Promise<void>;
  signOut: () => Promise<void>;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [session, setSession] = useState<Session | null>(null);
  const [loading, setLoading] = useState(true);
  const [isAdmin, setIsAdmin] = useState(false);

  useEffect(() => {
    let active = true;

    const {
      data: { subscription },
    } = supabase.auth.onAuthStateChange((_event, currentSession) => {
      if (active) {
        setSession(currentSession);
        setLoading(false);
      }
    });

    supabase.auth
      .getSession()
      .then(({ data, error }) => {
        if (!active) return;

        if (error) {
          setSession(null);
        } else {
          setSession(data.session);
        }

        setLoading(false);
      })
      .catch(() => {
        if (active) {
          setSession(null);
          setLoading(false);
        }
      });

    return () => {
      active = false;
      subscription.unsubscribe();
    };
  }, []);

  useEffect(() => {
    let active = true;

    async function checkAdmin() {
      setIsAdmin(false);

      if (!session?.user) return;

      const { data, error } = await supabase
        .from("admins")
        .select("user_id")
        .eq("user_id", session.user.id)
        .maybeSingle();

      if (!active) return;

      if (error || !data) {
        setIsAdmin(false);

        if (error) {
          console.error("Admin authorization check failed:", error.message);
        }

        return;
      }

      setIsAdmin(true);
    }

    void checkAdmin();

    return () => {
      active = false;
    };
  }, [session?.user.id]);

  async function signIn(email: string, password: string) {
    const { error } = await supabase.auth.signInWithPassword({
      email,
      password,
    });

    if (error) {
      throw new Error("Invalid email or password.");
    }
  }

  async function signOut() {
    const { error } = await supabase.auth.signOut();

    if (error) {
      throw new Error("Unable to sign out. Please try again.");
    }

    setIsAdmin(false);
  }

  return (
    <AuthContext.Provider
      value={{ session, loading, isAdmin, signIn, signOut }}
    >
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const context = useContext(AuthContext);

  if (!context) {
    throw new Error("useAuth must be used inside AuthProvider.");
  }

  return context;
}