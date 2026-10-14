import { useState } from "react";
import { useForm } from "react-hook-form";
import { z } from "zod";
import { zodResolver } from "@hookform/resolvers/zod";
import { Navigate } from "react-router-dom";
import { Eye, EyeOff, LockKeyhole, ShieldCheck } from "lucide-react";

import { useAuth } from "@/auth/AuthProvider";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";

const loginSchema = z.object({
  email: z.string().trim().email("Enter a valid email address."),
  password: z.string().min(1, "Password is required."),
});

type LoginFormValues = z.infer<typeof loginSchema>;

export default function LoginPage() {
  const { session, loading, isAdmin, signIn } = useAuth();
  const [showPassword, setShowPassword] = useState(false);
  const [submitError, setSubmitError] = useState("");

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<LoginFormValues>({
    resolver: zodResolver(loginSchema),
    defaultValues: {
      email: "",
      password: "",
    },
  });

  if (loading) {
    return (
      <main className="flex min-h-screen items-center justify-center">
        <p className="text-sm text-muted-foreground">Loading...</p>
      </main>
    );
  }

  if (session && isAdmin) {
    return <Navigate to="/" replace />;
  }

  async function onSubmit(values: LoginFormValues) {
    setSubmitError("");

    try {
      await signIn(values.email, values.password);
    } catch {
      setSubmitError("Invalid email or password, or this account is not authorized.");
    }
  }

  return (
    <main className="relative flex min-h-screen items-center justify-center overflow-hidden bg-background px-4 py-10">
      <div className="pointer-events-none absolute inset-0 bg-[radial-gradient(ellipse_at_top,_var(--tw-gradient-stops))] from-primary/10 via-background to-background" />

      <div className="relative grid w-full max-w-5xl overflow-hidden rounded-3xl border bg-card shadow-2xl md:grid-cols-2">
        <section className="hidden flex-col justify-between bg-primary p-10 text-primary-foreground md:flex">
          <div className="flex items-center gap-3">
            <div className="flex size-11 items-center justify-center rounded-xl bg-primary-foreground/15">
              <ShieldCheck className="size-6" />
            </div>
            <div>
              <p className="text-lg font-semibold tracking-tight">RKGIT ONE</p>
              <p className="text-sm text-primary-foreground/70">
                Administration Portal
              </p>
            </div>
          </div>

          <div className="space-y-5">
            <div className="inline-flex items-center gap-2 rounded-full border border-primary-foreground/20 bg-primary-foreground/10 px-3 py-1.5 text-xs font-medium">
              <LockKeyhole className="size-3.5" />
              Restricted access
            </div>

            <h1 className="text-4xl font-semibold leading-tight tracking-tight">
              Everything your campus needs.
              <br />
              One place to manage it.
            </h1>

            <p className="max-w-sm leading-7 text-primary-foreground/75">
              Manage study materials and campus events through one secure,
              centralized workspace.
            </p>
          </div>

          <p className="text-xs text-primary-foreground/60">
            RKGIT ONE · Admin access only
          </p>
        </section>

        <section className="flex items-center justify-center p-6 sm:p-10">
          <Card className="w-full max-w-md border-0 shadow-none">
            <CardHeader className="space-y-3 px-0">
              <div className="flex size-12 items-center justify-center rounded-xl bg-primary/10 text-primary md:hidden">
                <ShieldCheck className="size-6" />
              </div>

              <div className="space-y-2">
                <p className="text-sm font-medium text-primary">
                  RKGIT ONE ADMIN
                </p>
                <CardTitle className="text-3xl tracking-tight">
                  Welcome back
                </CardTitle>
                <CardDescription className="text-sm leading-6">
                  Sign in with your authorized administrator account to
                  continue.
                </CardDescription>
              </div>
            </CardHeader>

            <CardContent className="px-0 pt-4">
              <form onSubmit={handleSubmit(onSubmit)} className="space-y-5">
                <div className="space-y-2">
                  <Label htmlFor="email">Email address</Label>
                  <Input
                    id="email"
                    type="email"
                    autoComplete="username"
                    placeholder="admin@example.com"
                    aria-invalid={Boolean(errors.email)}
                    {...register("email")}
                  />
                  {errors.email && (
                    <p className="text-sm text-destructive">
                      {errors.email.message}
                    </p>
                  )}
                </div>

                <div className="space-y-2">
                  <div className="flex items-center justify-between gap-3">
                    <Label htmlFor="password">Password</Label>
                    <span className="text-xs text-muted-foreground">
                      Authorized admins only
                    </span>
                  </div>

                  <div className="relative">
                    <Input
                      id="password"
                      type={showPassword ? "text" : "password"}
                      autoComplete="current-password"
                      placeholder="Enter your password"
                      className="pr-11"
                      aria-invalid={Boolean(errors.password)}
                      {...register("password")}
                    />
                    <button
                      type="button"
                      aria-label={
                        showPassword ? "Hide password" : "Show password"
                      }
                      onClick={() => setShowPassword((current) => !current)}
                      className="absolute right-3 top-1/2 -translate-y-1/2 text-muted-foreground transition-colors hover:text-foreground"
                    >
                      {showPassword ? (
                        <EyeOff className="size-4" />
                      ) : (
                        <Eye className="size-4" />
                      )}
                    </button>
                  </div>

                  {errors.password && (
                    <p className="text-sm text-destructive">
                      {errors.password.message}
                    </p>
                  )}
                </div>

                {submitError && (
                  <div
                    role="alert"
                    className="rounded-lg border border-destructive/30 bg-destructive/5 px-3 py-2.5 text-sm text-destructive"
                  >
                    {submitError}
                  </div>
                )}

                <Button
                  type="submit"
                  className="h-11 w-full"
                  disabled={isSubmitting}
                >
                  {isSubmitting ? "Signing in..." : "Sign in securely"}
                </Button>

                <p className="text-center text-xs leading-5 text-muted-foreground">
                  This portal is restricted to authorized administrators.
                  Access attempts may be logged by the service.
                </p>
              </form>
            </CardContent>
          </Card>
        </section>
      </div>
    </main>
  );
}